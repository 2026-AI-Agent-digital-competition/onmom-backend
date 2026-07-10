package com.onmom.family.service;

import com.onmom.family.domain.FamilyConnection;
import com.onmom.family.domain.FamilyInviteCode;
import com.onmom.family.domain.FamilyInviteCodeStatus;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.family.repository.FamilyConnectionRepository;
import com.onmom.family.repository.FamilyInviteCodeRepository;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import com.onmom.pregnancy.repository.PregnancyRepository;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyInviteCodeTransactionalService {

    private final PregnancyRepository pregnancyRepository;
    private final UserRepository userRepository;
    private final FamilyInviteCodeRepository familyInviteCodeRepository;
    private final FamilyConnectionRepository familyConnectionRepository;

    public FamilyInviteCodeTransactionalService(
            PregnancyRepository pregnancyRepository,
            UserRepository userRepository,
            FamilyInviteCodeRepository familyInviteCodeRepository,
            FamilyConnectionRepository familyConnectionRepository
    ) {
        this.pregnancyRepository = pregnancyRepository;
        this.userRepository = userRepository;
        this.familyInviteCodeRepository = familyInviteCodeRepository;
        this.familyConnectionRepository = familyConnectionRepository;
    }

    @Transactional
    public IssueFamilyInviteCodeResponse issue(
            Long currentUserId,
            Long pregnancyId,
            String code,
            LocalDateTime expiresAt
    ) {
        User user = getActiveUser(currentUserId);
        if (!user.hasRole(UserRole.MOTHER)) {
            throw new BusinessException(ErrorCode.ROLE_ACCESS_DENIED);
        }

        Pregnancy pregnancy = pregnancyRepository.findByIdForUpdate(pregnancyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));
        if (pregnancy.getStatus() != PregnancyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }
        if (!pregnancy.isMother(currentUserId)) {
            throw new BusinessException(ErrorCode.PREGNANCY_ACCESS_DENIED);
        }

        familyInviteCodeRepository
                .findByPregnancyIdAndStatus(pregnancyId, FamilyInviteCodeStatus.PENDING)
                .forEach(FamilyInviteCode::revoke);

        FamilyInviteCode inviteCode = FamilyInviteCode.issue(pregnancyId, currentUserId, code, expiresAt);
        return IssueFamilyInviteCodeResponse.from(familyInviteCodeRepository.saveAndFlush(inviteCode));
    }

    @Transactional
    public AcceptResult accept(Long currentUserId, String code, LocalDateTime now) {
        User user = getActiveUser(currentUserId);
        if (!user.hasRole(UserRole.FAMILY)) {
            throw new BusinessException(ErrorCode.ROLE_ACCESS_DENIED);
        }

        FamilyInviteCode inviteCode = familyInviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        if (inviteCode.hasExpiredStatus()) {
            throw new BusinessException(ErrorCode.EXPIRED_INVITE_CODE);
        }
        if (!inviteCode.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }
        if (inviteCode.isExpired(now)) {
            inviteCode.expire();
            return AcceptResult.expiredResult();
        }
        if (inviteCode.getInviterUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_ACCEPT_OWN_INVITE);
        }

        Pregnancy pregnancy = pregnancyRepository.findById(inviteCode.getPregnancyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND));
        if (pregnancy.getStatus() != PregnancyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PREGNANCY_NOT_FOUND);
        }
        if (!pregnancy.getMotherUserId().equals(inviteCode.getInviterUserId())) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }

        FamilyConnection connection = familyConnectionRepository
                .findByPregnancyIdAndFamilyUserId(inviteCode.getPregnancyId(), currentUserId)
                .map(existingConnection -> {
                    existingConnection.connectAgain(now);
                    return existingConnection;
                })
                .orElseGet(() -> FamilyConnection.connect(
                        pregnancy.getId(),
                        pregnancy.getMotherUserId(),
                        currentUserId,
                        now
                ));
        return AcceptResult.success(AcceptFamilyInviteCodeResponse.from(
                familyConnectionRepository.saveAndFlush(connection)
        ));
    }

    @Transactional(readOnly = true)
    public Optional<AcceptFamilyInviteCodeResponse> findExistingConnection(String code, Long familyUserId) {
        return familyInviteCodeRepository.findByCode(code)
                .flatMap(inviteCode -> familyConnectionRepository.findByPregnancyIdAndFamilyUserId(
                        inviteCode.getPregnancyId(),
                        familyUserId
                ))
                .map(AcceptFamilyInviteCodeResponse::from);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public record AcceptResult(
            AcceptFamilyInviteCodeResponse response,
            boolean expired
    ) {

        static AcceptResult success(AcceptFamilyInviteCodeResponse response) {
            return new AcceptResult(response, false);
        }

        static AcceptResult expiredResult() {
            return new AcceptResult(null, true);
        }
    }
}
