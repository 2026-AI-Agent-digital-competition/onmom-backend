package com.onmom.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.auth.client.KakaoClient;
import com.onmom.auth.client.KakaoUserInfo;
import com.onmom.auth.dto.KakaoLoginRequest;
import com.onmom.auth.dto.KakaoLoginResponse;
import com.onmom.global.auth.JwtTokenProvider;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private KakaoClient kakaoClient;
    private OauthAccountService oauthAccountService;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService service;

    @BeforeEach
    void setUp() {
        kakaoClient = mock(KakaoClient.class);
        oauthAccountService = mock(OauthAccountService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        service = new AuthService(kakaoClient, oauthAccountService, jwtTokenProvider);
    }

    @Test
    void loginExchangesAuthorizationCodeBeforeFindingOrCreatingLocalUser() {
        KakaoLoginRequest request = new KakaoLoginRequest("authorization-code", "mother");
        KakaoUserInfo kakaoUserInfo = kakaoUserInfo();
        User user = user(1L, "온맘", UserRole.MOTHER);
        when(kakaoClient.authenticate("authorization-code")).thenReturn(kakaoUserInfo);
        when(oauthAccountService.findOrCreate(kakaoUserInfo, UserRole.MOTHER)).thenReturn(user);
        when(jwtTokenProvider.createAccessToken(1L, UserRole.MOTHER)).thenReturn("onmom-token");
        when(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        KakaoLoginResponse response = service.loginWithKakao(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessToken()).isEqualTo("onmom-token");
        assertThat(response.role()).isEqualTo("MOTHER");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        InOrder loginOrder = inOrder(kakaoClient, oauthAccountService);
        loginOrder.verify(kakaoClient).authenticate("authorization-code");
        loginOrder.verify(oauthAccountService).findOrCreate(kakaoUserInfo, UserRole.MOTHER);
        verify(kakaoClient).authenticate("authorization-code");
    }

    @Test
    void loginRetriesExistingUserAfterUniqueConstraintConflict() {
        KakaoLoginRequest request = new KakaoLoginRequest("authorization-code", "mother");
        KakaoUserInfo kakaoUserInfo = kakaoUserInfo();
        User concurrentWinner = user(2L, "먼저 가입한 사용자", UserRole.FAMILY);
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("duplicate oauth account");
        when(kakaoClient.authenticate("authorization-code")).thenReturn(kakaoUserInfo);
        when(oauthAccountService.findOrCreate(kakaoUserInfo, UserRole.MOTHER)).thenThrow(conflict);
        when(oauthAccountService.findExisting("kakao-1")).thenReturn(Optional.of(concurrentWinner));
        when(jwtTokenProvider.createAccessToken(2L, UserRole.FAMILY)).thenReturn("winner-token");
        when(jwtTokenProvider.getAccessTokenExpiresInSeconds()).thenReturn(3600L);

        KakaoLoginResponse response = service.loginWithKakao(request);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo("FAMILY");
        assertThat(response.accessToken()).isEqualTo("winner-token");
        verify(oauthAccountService).findExisting("kakao-1");
        verify(kakaoClient).authenticate("authorization-code");
    }

    @Test
    void loginRethrowsOriginalConflictWhenAccountCannotBeFound() {
        KakaoLoginRequest request = new KakaoLoginRequest("authorization-code", "mother");
        KakaoUserInfo kakaoUserInfo = kakaoUserInfo();
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("unexpected integrity violation");
        when(kakaoClient.authenticate("authorization-code")).thenReturn(kakaoUserInfo);
        when(oauthAccountService.findOrCreate(kakaoUserInfo, UserRole.MOTHER)).thenThrow(conflict);
        when(oauthAccountService.findExisting("kakao-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginWithKakao(request))
                .isSameAs(conflict);
    }

    private KakaoUserInfo kakaoUserInfo() {
        return new KakaoUserInfo("kakao-1", "onmom@example.com", "온맘", "https://example.com/profile.png");
    }

    private User user(Long id, String nickname, UserRole role) {
        User user = User.create(nickname, "https://example.com/profile.png", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
