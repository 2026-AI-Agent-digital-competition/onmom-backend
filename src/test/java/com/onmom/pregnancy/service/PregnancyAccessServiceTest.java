package com.onmom.pregnancy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.onmom.family.domain.FamilyConnectionStatus;
import com.onmom.family.repository.FamilyConnectionRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.repository.PregnancyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PregnancyAccessServiceTest {

    private PregnancyRepository pregnancyRepository;
    private FamilyConnectionRepository familyConnectionRepository;
    private PregnancyAccessService service;

    @BeforeEach
    void setUp() {
        pregnancyRepository = mock(PregnancyRepository.class);
        familyConnectionRepository = mock(FamilyConnectionRepository.class);
        service = new PregnancyAccessService(pregnancyRepository, familyConnectionRepository);
    }

    @Test
    void motherCanAccessOwnPregnancy() {
        Pregnancy pregnancy = pregnancy(1L, 10L);
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));

        assertThat(service.getAccessiblePregnancy(10L, 1L)).isSameAs(pregnancy);
        verifyNoInteractions(familyConnectionRepository);
    }

    @Test
    void connectedFamilyCanAccessPregnancy() {
        Pregnancy pregnancy = pregnancy(1L, 10L);
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));
        when(familyConnectionRepository.existsByPregnancyIdAndFamilyUserIdAndStatus(
                1L,
                20L,
                FamilyConnectionStatus.CONNECTED
        )).thenReturn(true);

        assertThat(service.getAccessiblePregnancy(20L, 1L)).isSameAs(pregnancy);
    }

    @Test
    void unconnectedUserIsDenied() {
        Pregnancy pregnancy = pregnancy(1L, 10L);
        when(pregnancyRepository.findById(1L)).thenReturn(Optional.of(pregnancy));

        assertThatThrownBy(() -> service.getAccessiblePregnancy(20L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PREGNANCY_ACCESS_DENIED);
    }

    private Pregnancy pregnancy(Long id, Long motherUserId) {
        Pregnancy pregnancy = Pregnancy.create(motherUserId, "온맘", null, null, null, null);
        ReflectionTestUtils.setField(pregnancy, "id", id);
        return pregnancy;
    }
}
