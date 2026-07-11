package com.onmom.pregnancy.controller;

import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import com.onmom.pregnancy.dto.CreatePregnancyRequest;
import com.onmom.pregnancy.dto.PregnancyResponse;
import com.onmom.pregnancy.service.PregnancyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pregnancies")
public class PregnancyController {

    private final PregnancyService pregnancyService;

    public PregnancyController(PregnancyService pregnancyService) {
        this.pregnancyService = pregnancyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PregnancyResponse>> create(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CreatePregnancyRequest request
    ) {
        PregnancyResponse response = pregnancyService.create(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
