package com.onmom.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.family.domain.FamilyConnection;
import com.onmom.family.domain.FamilyConnectionStatus;
import com.onmom.family.domain.FamilyInviteCode;
import com.onmom.family.domain.FamilyInviteCodeStatus;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.family.repository.FamilyConnectionRepository;
import com.onmom.family.repository.FamilyInviteCodeRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FamilyInviteCodeTransactionalServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 11, 0, 0);

    private PregnancyRepository pregnancyRepository;
    private UserRepository userRepository;
    private FamilyInviteCodeRepository inviteCodeRepository;
    private FamilyConnectionRepository connectionRepository;
    private FamilyInviteCodeTransactionalService service;

    @BeforeEach
    void setUp() {
        pregnancyRepository = mock(PregnancyRepository.class);
        userRepository = mock(UserRepository.class);
        inviteCodeRepository = mock(FamilyInviteCodeRepository.class);
        connectionRepository = mock(FamilyConnectionRepository.class);
        service = new FamilyInviteCodeTransactionalService(
                pregnancyRepository,
                userRepository,
                inviteCodeRepository,
                connectionRepository
        );
    }

    @Test
    void issueRevokesPendingCodeAndFlushesNewCode() {
        User mother = User.create("온맘", null, UserRole.MOTHER);
        Pregnancy pregnancy = pregnancy(1L, 10L);
        FamilyInviteCode pending = FamilyInviteCode.issue(1L, 10L, "OLD234", NOW.plusMinutes(5));
        FamilyInviteCode created = FamilyInviteCode.issue(1L, 10L, "ABC234", NOW.plusMinutes(10));
        when(userRepository.findById(10L)).thenReturn(Optional.of(mother));
        when(pregnancyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pregnancy));
        when(inviteCodeRepository.findByPregnancyIdAndStatus(1L, FamilyInviteCodeStatus.PENDING))
                .thenReturn(List.of(pending));
        when(inviteCodeRepository.saveAndFlush(any(FamilyInviteCode.class))).thenReturn(created);

        IssueFamilyInviteCodeResponse response = service.issue(10L, 1L, "ABC234", NOW.plusMinutes(10));

        assertThat(pending.getStatus()).isEqualTo(FamilyInviteCodeStatus.REVOKED);
        assertThat(response.code()).isEqualTo("ABC234");
    }

    @Test
    void issueRejectsFamilyRole() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));

        assertErrorCode(
                () -> service.issue(20L, 1L, "ABC234", NOW.plusMinutes(10)),
                ErrorCode.ROLE_ACCESS_DENIED
        );
        verify(pregnancyRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void issueRejectsPregnancyOwnedByAnotherMother() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.create("온맘", null, UserRole.MOTHER)));
        when(pregnancyRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pregnancy(1L, 11L)));

        assertErrorCode(
                () -> service.issue(10L, 1L, "ABC234", NOW.plusMinutes(10)),
                ErrorCode.PREGNANCY_ACCESS_DENIED
        );
    }

    @Test
    void acceptCreatesConnectionForFamily() {
        FamilyInviteCode inviteCode = FamilyInviteCode.issue(1L, 10L, "ABC234", NOW.plusMinutes(5));
        Pregnancy pregnancy = pregnancy(1L, 10L);
        FamilyConnection savedConnection = FamilyConnection.connect(1L, 10L, 20L, NOW);
        ReflectionTestUtils.setField(savedConnection, "id", 30L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));
        when(inviteCodeRepository.findByCode("ABC234")).thenReturn(Optional.of(inviteCode));
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));
        when(connectionRepository.findByPregnancyIdAndFamilyUserId(1L, 20L)).thenReturn(Optional.empty());
        when(connectionRepository.saveAndFlush(any(FamilyConnection.class))).thenReturn(savedConnection);

        FamilyInviteCodeTransactionalService.AcceptResult result = service.accept(20L, "ABC234", NOW);

        assertThat(result.expired()).isFalse();
        assertThat(result.response()).isEqualTo(new AcceptFamilyInviteCodeResponse(1L, 30L, "CONNECTED"));
    }

    @Test
    void acceptRejectsMotherRole() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.create("온맘", null, UserRole.MOTHER)));

        assertErrorCode(
                () -> service.accept(10L, "ABC234", NOW),
                ErrorCode.ROLE_ACCESS_DENIED
        );
        verify(inviteCodeRepository, never()).findByCode(any());
    }

    @Test
    void acceptRejectsOwnInvite() {
        FamilyInviteCode inviteCode = FamilyInviteCode.issue(1L, 20L, "ABC234", NOW.plusMinutes(5));
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));
        when(inviteCodeRepository.findByCode("ABC234")).thenReturn(Optional.of(inviteCode));

        assertErrorCode(
                () -> service.accept(20L, "ABC234", NOW),
                ErrorCode.CANNOT_ACCEPT_OWN_INVITE
        );
    }

    @Test
    void acceptMarksExpiredCodeWithoutThrowingInsideTransaction() {
        FamilyInviteCode inviteCode = FamilyInviteCode.issue(1L, 10L, "ABC234", NOW.minusSeconds(1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));
        when(inviteCodeRepository.findByCode("ABC234")).thenReturn(Optional.of(inviteCode));

        FamilyInviteCodeTransactionalService.AcceptResult result = service.accept(20L, "ABC234", NOW);

        assertThat(result.expired()).isTrue();
        assertThat(inviteCode.getStatus()).isEqualTo(FamilyInviteCodeStatus.EXPIRED);
        verify(connectionRepository, never()).saveAndFlush(any(FamilyConnection.class));
    }

    @Test
    void acceptRejectsInviteForInactivePregnancy() {
        FamilyInviteCode inviteCode = FamilyInviteCode.issue(1L, 10L, "ABC234", NOW.plusMinutes(5));
        Pregnancy pregnancy = pregnancy(1L, 10L);
        ReflectionTestUtils.setField(pregnancy, "status", com.onmom.pregnancy.domain.PregnancyStatus.ENDED);
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));
        when(inviteCodeRepository.findByCode("ABC234")).thenReturn(Optional.of(inviteCode));
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));

        assertErrorCode(
                () -> service.accept(20L, "ABC234", NOW),
                ErrorCode.PREGNANCY_NOT_FOUND
        );
    }

    @Test
    void acceptReconnectsRevokedConnection() {
        FamilyInviteCode inviteCode = FamilyInviteCode.issue(1L, 10L, "ABC234", NOW.plusMinutes(5));
        Pregnancy pregnancy = pregnancy(1L, 10L);
        FamilyConnection connection = FamilyConnection.connect(1L, 10L, 20L, NOW.minusDays(1));
        ReflectionTestUtils.setField(connection, "id", 30L);
        ReflectionTestUtils.setField(connection, "status", FamilyConnectionStatus.REVOKED);
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.create("가족", null, UserRole.FAMILY)));
        when(inviteCodeRepository.findByCode("ABC234")).thenReturn(Optional.of(inviteCode));
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));
        when(connectionRepository.findByPregnancyIdAndFamilyUserId(1L, 20L))
                .thenReturn(Optional.of(connection));
        when(connectionRepository.saveAndFlush(connection)).thenReturn(connection);

        FamilyInviteCodeTransactionalService.AcceptResult result = service.accept(20L, "ABC234", NOW);

        assertThat(connection.getStatus()).isEqualTo(FamilyConnectionStatus.CONNECTED);
        assertThat(result.response().connectionId()).isEqualTo(30L);
    }

    private Pregnancy pregnancy(Long id, Long motherUserId) {
        Pregnancy pregnancy = Pregnancy.create(motherUserId, "온맘", null, null, null, null);
        ReflectionTestUtils.setField(pregnancy, "id", id);
        return pregnancy;
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
