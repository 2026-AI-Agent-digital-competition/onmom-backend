package com.onmom.auth.dto;

public record DevAuthStatusResponse(
        boolean enabled,
        boolean jwtSecretConfigured
) {
}
