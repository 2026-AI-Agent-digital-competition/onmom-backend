package com.onmom.auth.dto;

public record DevAccessTokenResponse(
        Long userId,
        String role,
        String tokenType,
        String accessToken,
        long expiresIn
) {
}
