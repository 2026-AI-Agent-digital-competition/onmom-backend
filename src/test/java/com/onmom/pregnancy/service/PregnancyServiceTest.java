package com.onmom.pregnancy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.dto.CreatePregnancyRequest;
import com.onmom.pregnancy.dto.PregnancyResponse;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PregnancyServiceTest {

    private UserRepository userRepository;
    private PregnancyRepository pregnancyRepository;
    private PregnancyService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        pregnancyRepository = mock(PregnancyRepository.class);
        service = new PregnancyService(userRepository, pregnancyRepository);
    }

    @Test
    void createsActivePregnancyForMother() {
        User mother = User.create("온맘", null, UserRole.MOTHER);
        Pregnancy saved = Pregnancy.create(1L, "온맘", "튼튼이", 12, 13, LocalDate.of(2027, 1, 1));
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mother));
        when(pregnancyRepository.existsByMotherUserIdAndStatus(1L, PregnancyStatus.ACTIVE)).thenReturn(false);
        when(pregnancyRepository.save(any(Pregnancy.class))).thenReturn(saved);

        PregnancyResponse response = service.create(
                1L,
                new CreatePregnancyRequest("온맘", "튼튼이", 12, 13, LocalDate.of(2027, 1, 1))
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(pregnancyRepository).save(any(Pregnancy.class));
    }

    @Test
    void rejectsFamilyRole() {
        User family = User.create("가족", null, UserRole.FAMILY);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(family));

        assertErrorCode(
                () -> service.create(2L, new CreatePregnancyRequest("가족", null, null, null, null)),
                ErrorCode.ROLE_ACCESS_DENIED
        );
        verify(pregnancyRepository, never()).save(any(Pregnancy.class));
    }

    @Test
    void rejectsSecondActivePregnancy() {
        User mother = User.create("온맘", null, UserRole.MOTHER);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mother));
        when(pregnancyRepository.existsByMotherUserIdAndStatus(1L, PregnancyStatus.ACTIVE)).thenReturn(true);

        assertErrorCode(
                () -> service.create(1L, new CreatePregnancyRequest("온맘", null, null, null, null)),
                ErrorCode.ACTIVE_PREGNANCY_ALREADY_EXISTS
        );
        verify(pregnancyRepository, never()).save(any(Pregnancy.class));
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
