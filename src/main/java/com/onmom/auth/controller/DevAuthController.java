package com.onmom.auth.controller;

import com.onmom.auth.dto.CreateDevAccessTokenRequest;
import com.onmom.auth.dto.DevAuthStatusResponse;
import com.onmom.auth.dto.DevAccessTokenResponse;
import com.onmom.global.auth.JwtTokenProvider;
import com.onmom.global.auth.JwtProperties;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.global.response.ApiResponse;
import com.onmom.user.domain.User;
import com.onmom.user.domain.UserRole;
import com.onmom.user.domain.UserStatus;
import com.onmom.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev/auth")
@ConditionalOnProperty(name = "onmom.dev-token.enabled", havingValue = "true")
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public DevAuthController(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<DevAuthStatusResponse>> status() {
        DevAuthStatusResponse response = new DevAuthStatusResponse(
                true,
                StringUtils.hasText(jwtProperties.getSecret())
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/access-token")
    public ResponseEntity<ApiResponse<DevAccessTokenResponse>> createAccessToken(
            @Valid @RequestBody CreateDevAccessTokenRequest request
    ) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserRole role = user.getPrimaryRole();
        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        DevAccessTokenResponse response = new DevAccessTokenResponse(
                user.getId(),
                role.name(),
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
