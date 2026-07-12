package com.onmom.family.controller;

import com.onmom.family.dto.CreateFamilyInsightRequest;
import com.onmom.family.dto.FamilyInsightResponse;
import com.onmom.family.service.FamilyInsightService;
import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/family-insights")
public class FamilyInsightController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final FamilyInsightService familyInsightService;

    public FamilyInsightController(FamilyInsightService familyInsightService) {
        this.familyInsightService = familyInsightService;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<FamilyInsightResponse>> create(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CreateFamilyInsightRequest request
    ) {
        FamilyInsightResponse response = familyInsightService.create(currentUserId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
