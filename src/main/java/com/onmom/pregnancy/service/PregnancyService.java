package com.onmom.pregnancy.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PregnancyService {

    private final UserRepository userRepository;
    private final PregnancyRepository pregnancyRepository;

    public PregnancyService(UserRepository userRepository, PregnancyRepository pregnancyRepository) {
        this.userRepository = userRepository;
        this.pregnancyRepository = pregnancyRepository;
    }

    @Transactional
    public PregnancyResponse create(Long currentUserId, CreatePregnancyRequest request) {
        User user = userRepository.findByIdForUpdate(currentUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasRole(UserRole.MOTHER)) {
            throw new BusinessException(ErrorCode.ROLE_ACCESS_DENIED);
        }
        if (pregnancyRepository.existsByMotherUserIdAndStatus(currentUserId, PregnancyStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.ACTIVE_PREGNANCY_ALREADY_EXISTS);
        }

        Pregnancy pregnancy = Pregnancy.create(
                currentUserId,
                request.motherDisplayName(),
                request.babyNickname(),
                request.pregnancyWeekStart(),
                request.pregnancyWeekEnd(),
                request.dueDate()
        );
        return PregnancyResponse.from(pregnancyRepository.save(pregnancy));
    }
}
