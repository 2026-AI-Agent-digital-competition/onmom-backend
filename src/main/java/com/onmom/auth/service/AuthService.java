package com.onmom.auth.service;

import com.onmom.auth.client.KakaoClient;
import com.onmom.auth.client.KakaoUserInfo;
import com.onmom.auth.dto.KakaoLoginRequest;
import com.onmom.auth.dto.KakaoLoginResponse;
import com.onmom.global.auth.JwtTokenProvider;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoClient kakaoClient;
    private final OauthAccountService oauthAccountService;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoLoginResponse loginWithKakao(KakaoLoginRequest request) {
        UserRole requestedRole = UserRole.from(request.role());
        KakaoUserInfo kakaoUserInfo = kakaoClient.authenticate(request.authorizationCode());

        User user;
        try {
            user = oauthAccountService.findOrCreate(kakaoUserInfo, requestedRole);
        } catch (DataIntegrityViolationException exception) {
            user = oauthAccountService.findExisting(kakaoUserInfo.providerUserId())
                    .orElseThrow(() -> exception);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getPrimaryRole());
        return KakaoLoginResponse.of(user, accessToken, jwtTokenProvider.getAccessTokenExpiresInSeconds());
    }
}
