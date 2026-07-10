package com.onmom.auth.dto;

import com.onmom.user.domain.User;

public record KakaoLoginResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role,
        String tokenType,
        String accessToken,
        long expiresIn
) {

    public static KakaoLoginResponse of(User user, String accessToken, long expiresIn) {
        return new KakaoLoginResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getPrimaryRole().name(),
                "Bearer",
                accessToken,
                expiresIn
        );
    }
}
