package com.onmom.auth.controller;

import com.onmom.auth.dto.KakaoLoginRequest;
import com.onmom.auth.dto.KakaoLoginResponse;
import com.onmom.auth.service.AuthService;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao-login")
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        KakaoLoginResponse response = authService.loginWithKakao(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
