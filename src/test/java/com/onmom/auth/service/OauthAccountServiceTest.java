package com.onmom.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.onmom.auth.client.KakaoUserInfo;
import com.onmom.user.domain.OauthAccount;
import com.onmom.user.domain.OauthProvider;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.OauthAccountRepository;
import com.onmom.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class OauthAccountServiceTest {

    private UserRepository userRepository;
    private OauthAccountRepository oauthAccountRepository;
    private OauthAccountService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        oauthAccountRepository = mock(OauthAccountRepository.class);
        service = new OauthAccountService(userRepository, oauthAccountRepository);
    }

    @Test
    void findOrCreateReturnsExistingUserWithoutSaving() {
        User existingUser = user(1L, "온맘", UserRole.MOTHER);
        OauthAccount existingAccount = OauthAccount.createKakao(existingUser, "kakao-1", "onmom@example.com");
        KakaoUserInfo kakaoUserInfo = kakaoUserInfo();
        when(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.KAKAO, "kakao-1"))
                .thenReturn(Optional.of(existingAccount));

        User result = service.findOrCreate(kakaoUserInfo, UserRole.FAMILY);

        assertThat(result).isSameAs(existingUser);
        verifyNoInteractions(userRepository);
        verify(oauthAccountRepository, never()).saveAndFlush(any(OauthAccount.class));
    }

    @Test
    void findOrCreateSavesUserBeforeFlushingOauthAccount() {
        User savedUser = user(1L, "온맘", UserRole.MOTHER);
        KakaoUserInfo kakaoUserInfo = kakaoUserInfo();
        when(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.KAKAO, "kakao-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = service.findOrCreate(kakaoUserInfo, UserRole.MOTHER);

        assertThat(result).isSameAs(savedUser);
        InOrder saveOrder = inOrder(userRepository, oauthAccountRepository);
        saveOrder.verify(userRepository).save(any(User.class));
        saveOrder.verify(oauthAccountRepository).saveAndFlush(any(OauthAccount.class));
    }

    @Test
    void findExistingReturnsUserMappedToKakaoAccount() {
        User existingUser = user(1L, "온맘", UserRole.MOTHER);
        OauthAccount existingAccount = OauthAccount.createKakao(existingUser, "kakao-1", "onmom@example.com");
        when(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.KAKAO, "kakao-1"))
                .thenReturn(Optional.of(existingAccount));

        Optional<User> result = service.findExisting("kakao-1");

        assertThat(result).containsSame(existingUser);
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
