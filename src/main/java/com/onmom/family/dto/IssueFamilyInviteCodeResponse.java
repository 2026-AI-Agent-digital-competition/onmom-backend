package com.onmom.family.dto;

import com.onmom.family.domain.FamilyInviteCode;
import java.time.LocalDateTime;

public record IssueFamilyInviteCodeResponse(
        String code,
        LocalDateTime expiresAt
) {

    public static IssueFamilyInviteCodeResponse from(FamilyInviteCode inviteCode) {
        return new IssueFamilyInviteCodeResponse(inviteCode.getCode(), inviteCode.getExpiresAt());
    }
}
