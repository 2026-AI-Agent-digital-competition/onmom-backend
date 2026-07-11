package com.onmom.auth.client;

public record KakaoUserInfo(
        String providerUserId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
