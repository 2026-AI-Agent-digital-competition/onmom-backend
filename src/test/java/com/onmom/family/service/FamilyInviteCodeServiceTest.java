package com.onmom.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.family.dto.AcceptFamilyInviteCodeRequest;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FamilyInviteCodeServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-11T00:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 11, 0, 0);

    private FamilyInviteCodeTransactionalService transactionalService;
    private FamilyInviteCodeGenerator codeGenerator;
    private FamilyInviteCodeService service;

    @BeforeEach
    void setUp() {
        transactionalService = mock(FamilyInviteCodeTransactionalService.class);
        codeGenerator = mock(FamilyInviteCodeGenerator.class);
        service = new FamilyInviteCodeService(transactionalService, codeGenerator, FIXED_CLOCK);
    }

    @Test
    void issueDelegatesWithTenMinuteExpiration() {
        IssueFamilyInviteCodeResponse expected = new IssueFamilyInviteCodeResponse("ABC234", NOW.plusMinutes(10));
        when(codeGenerator.generate()).thenReturn("ABC234");
        when(transactionalService.issue(10L, 1L, "ABC234", NOW.plusMinutes(10))).thenReturn(expected);

        IssueFamilyInviteCodeResponse response = service.issue(10L, 1L);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void issueRetriesAfterInviteCodeUniqueConflict() {
        IssueFamilyInviteCodeResponse expected = new IssueFamilyInviteCodeResponse("XYZ789", NOW.plusMinutes(10));
        when(codeGenerator.generate()).thenReturn("ABC234", "XYZ789");
        when(transactionalService.issue(10L, 1L, "ABC234", NOW.plusMinutes(10)))
                .thenThrow(constraintViolation("uk_family_invite_code"));
        when(transactionalService.issue(10L, 1L, "XYZ789", NOW.plusMinutes(10))).thenReturn(expected);

        assertThat(service.issue(10L, 1L)).isEqualTo(expected);
    }

    @Test
    void acceptThrowsExpiredErrorAfterTransactionalStatusUpdate() {
        when(transactionalService.accept(20L, "ABC234", NOW))
                .thenReturn(FamilyInviteCodeTransactionalService.AcceptResult.expiredResult());

        assertErrorCode(
                () -> service.accept(20L, new AcceptFamilyInviteCodeRequest("abc234")),
                ErrorCode.EXPIRED_INVITE_CODE
        );
    }

    @Test
    void acceptReturnsExistingConnectionAfterUniqueConflict() {
        AcceptFamilyInviteCodeResponse expected = new AcceptFamilyInviteCodeResponse(1L, 30L, "CONNECTED");
        when(transactionalService.accept(20L, "ABC234", NOW))
                .thenThrow(constraintViolation("uk_family_connection"));
        when(transactionalService.findExistingConnection("ABC234", 20L)).thenReturn(Optional.of(expected));

        AcceptFamilyInviteCodeResponse response = service.accept(
                20L,
                new AcceptFamilyInviteCodeRequest("abc234")
        );

        assertThat(response).isEqualTo(expected);
        verify(transactionalService).findExistingConnection("ABC234", 20L);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "duplicate",
                new SQLException("duplicate"),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate", cause);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
