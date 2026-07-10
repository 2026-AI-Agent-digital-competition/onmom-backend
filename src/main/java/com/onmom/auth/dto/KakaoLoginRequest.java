package com.onmom.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank(message = "authorizationCode는 필수입니다.")
        String authorizationCode,

        @NotBlank(message = "role은 필수입니다.")
        String role
) {
}
