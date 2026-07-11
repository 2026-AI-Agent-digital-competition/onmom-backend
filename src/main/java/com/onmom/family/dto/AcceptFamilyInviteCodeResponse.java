package com.onmom.family.dto;

import com.onmom.family.domain.FamilyConnection;

public record AcceptFamilyInviteCodeResponse(
        Long pregnancyId,
        Long connectionId,
        String status
) {

    public static AcceptFamilyInviteCodeResponse from(FamilyConnection connection) {
        return new AcceptFamilyInviteCodeResponse(
                connection.getPregnancyId(),
                connection.getId(),
                connection.getStatus().name()
        );
    }
}
