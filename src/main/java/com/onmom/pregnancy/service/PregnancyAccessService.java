package com.onmom.pregnancy.service;

import com.onmom.family.domain.FamilyConnectionStatus;
import com.onmom.family.repository.FamilyConnectionRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.repository.PregnancyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PregnancyAccessService {

    private final PregnancyRepository pregnancyRepository;
    private final FamilyConnectionRepository familyConnectionRepository;

    public PregnancyAccessService(
            PregnancyRepository pregnancyRepository,
            FamilyConnectionRepository familyConnectionRepository
    ) {
        this.pregnancyRepository = pregnancyRepository;
        this.familyConnectionRepository = familyConnectionRepository;
    }

    @Transactional(readOnly = true)
    public Pregnancy getAccessiblePregnancy(Long currentUserId, Long pregnancyId) {
        Pregnancy pregnancy = pregnancyRepository.findById(pregnancyId)
                .filter(candidate -> candidate.getStatus() == PregnancyStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));
        if (pregnancy.isMother(currentUserId)) {
            return pregnancy;
        }
        if (familyConnectionRepository.existsByPregnancyIdAndFamilyUserIdAndStatus(
                pregnancyId,
                currentUserId,
                FamilyConnectionStatus.CONNECTED
        )) {
            return pregnancy;
        }
        throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
    }
}
