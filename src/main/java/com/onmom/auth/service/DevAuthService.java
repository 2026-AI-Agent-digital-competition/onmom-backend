package com.onmom.auth.service;

import com.onmom.auth.dto.DevAccessTokenResponse;
import com.onmom.auth.dto.DevAuthStatusResponse;
import com.onmom.global.auth.JwtProperties;
import com.onmom.global.auth.JwtTokenProvider;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class DevAuthService {

    private static final String DEMO_MOTHER_NICKNAME = "온맘 데모 산모";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public DevAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public DevAuthStatusResponse status() {
        return new DevAuthStatusResponse(true, StringUtils.hasText(jwtProperties.getSecret()));
    }

    public DevAccessTokenResponse createAccessToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return issueAccessToken(user);
    }

    @Transactional
    public DevAccessTokenResponse loginAsDemoMother() {
        User user = userRepository
                .findFirstByNicknameAndPrimaryRoleOrderByIdAsc(DEMO_MOTHER_NICKNAME, UserRole.MOTHER)
                .filter(User::isActive)
                .orElseGet(() -> userRepository.save(User.create(DEMO_MOTHER_NICKNAME, null, UserRole.MOTHER)));
        return issueAccessToken(user);
    }

    private DevAccessTokenResponse issueAccessToken(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getPrimaryRole() == null) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getPrimaryRole());
        return new DevAccessTokenResponse(
                user.getId(),
                user.getPrimaryRole().name(),
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds()
        );
    }
}
