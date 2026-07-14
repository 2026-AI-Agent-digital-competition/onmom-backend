package com.onmom.auth.controller;

import com.onmom.auth.dto.CreateDevAccessTokenRequest;
import com.onmom.auth.dto.DevAuthStatusResponse;
import com.onmom.auth.dto.DevAccessTokenResponse;
import com.onmom.auth.service.DevAuthService;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev/auth")
@ConditionalOnProperty(name = "onmom.dev-token.enabled", havingValue = "true")
public class DevAuthController {

    private final DevAuthService devAuthService;

    public DevAuthController(DevAuthService devAuthService) {
        this.devAuthService = devAuthService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<DevAuthStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(devAuthService.status()));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<ApiResponse<DevAccessTokenResponse>> demoLogin() {
        return ResponseEntity.ok(ApiResponse.success(devAuthService.loginAsDemoMother()));
    }

    @PostMapping("/access-token")
    public ResponseEntity<ApiResponse<DevAccessTokenResponse>> createAccessToken(
            @Valid @RequestBody CreateDevAccessTokenRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(devAuthService.createAccessToken(request.userId())));
    }
}
