package com.onmom.auth.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDevAccessTokenRequest(
        @NotNull Long userId
) {
}
