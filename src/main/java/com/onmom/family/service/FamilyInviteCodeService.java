package com.onmom.family.service;

import com.onmom.family.dto.AcceptFamilyInviteCodeRequest;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class FamilyInviteCodeService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;
    private static final long INVITE_CODE_EXPIRATION_MINUTES = 10L;
    private static final String INVITE_CODE_CONSTRAINT = "uk_family_invite_code";
    private static final String FAMILY_CONNECTION_CONSTRAINT = "uk_family_connection";

    private final FamilyInviteCodeTransactionalService transactionalService;
    private final FamilyInviteCodeGenerator codeGenerator;
    private final Clock clock;

    public FamilyInviteCodeService(
            FamilyInviteCodeTransactionalService transactionalService,
            FamilyInviteCodeGenerator codeGenerator,
            Clock clock
    ) {
        this.transactionalService = transactionalService;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    public IssueFamilyInviteCodeResponse issue(Long currentUserId, Long pregnancyId) {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            LocalDateTime now = LocalDateTime.now(clock);
            try {
                return transactionalService.issue(
                        currentUserId,
                        pregnancyId,
                        codeGenerator.generate(),
                        now.plusMinutes(INVITE_CODE_EXPIRATION_MINUTES)
                );
            } catch (DataIntegrityViolationException exception) {
                if (!isConstraintViolation(exception, INVITE_CODE_CONSTRAINT)) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Failed to generate unique family invite code.");
    }

    public AcceptFamilyInviteCodeResponse accept(Long currentUserId, AcceptFamilyInviteCodeRequest request) {
        String normalizedCode = normalizeCode(request.code());
        FamilyInviteCodeTransactionalService.AcceptResult result;
        try {
            result = transactionalService.accept(currentUserId, normalizedCode, LocalDateTime.now(clock));
        } catch (DataIntegrityViolationException exception) {
            if (!isConstraintViolation(exception, FAMILY_CONNECTION_CONSTRAINT)) {
                throw exception;
            }
            return transactionalService.findExistingConnection(normalizedCode, currentUserId)
                    .orElseThrow(() -> exception);
        }
        if (result.expired()) {
            throw new BusinessException(ErrorCode.EXPIRED_INVITE_CODE);
        }
        return result.response();
    }

    private String normalizeCode(String code) {
        return code.toUpperCase(Locale.ROOT);
    }

    private boolean isConstraintViolation(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return constraintName.equals(constraintViolation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}
