package com.onmom.auth.service;

import com.onmom.auth.client.KakaoUserInfo;
import com.onmom.user.domain.OauthAccount;
import com.onmom.user.domain.OauthProvider;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.repository.OauthAccountRepository;
import com.onmom.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OauthAccountService {

    private final UserRepository userRepository;
    private final OauthAccountRepository oauthAccountRepository;

    @Transactional
    public User findOrCreate(KakaoUserInfo kakaoUserInfo, UserRole role) {
        return oauthAccountRepository
                .findByProviderAndProviderUserId(OauthProvider.KAKAO, kakaoUserInfo.providerUserId())
                .map(OauthAccount::getUser)
                .orElseGet(() -> createUserAndOauthAccount(kakaoUserInfo, role));
    }

    @Transactional(readOnly = true)
    public Optional<User> findExisting(String providerUserId) {
        return oauthAccountRepository
                .findByProviderAndProviderUserId(OauthProvider.KAKAO, providerUserId)
                .map(OauthAccount::getUser);
    }

    private User createUserAndOauthAccount(KakaoUserInfo kakaoUserInfo, UserRole role) {
        User user = userRepository.save(User.create(
                kakaoUserInfo.nickname(),
                kakaoUserInfo.profileImageUrl(),
                role
        ));
        oauthAccountRepository.saveAndFlush(
                OauthAccount.createKakao(user, kakaoUserInfo.providerUserId(), kakaoUserInfo.email())
        );
        return user;
    }
}
