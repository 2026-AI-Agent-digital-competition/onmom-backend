package com.onmom.family.controller;

import com.onmom.family.dto.FamilyMessageListResponse;
import com.onmom.family.service.FamilyMessageService;
import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/family-messages")
public class FamilyMessageController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final FamilyMessageService familyMessageService;

    public FamilyMessageController(FamilyMessageService familyMessageService) {
        this.familyMessageService = familyMessageService;
    }

    @GetMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<FamilyMessageListResponse>> findReceived(
            @CurrentUserId Long currentUserId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        FamilyMessageListResponse response = familyMessageService.findReceived(currentUserId, cursor, size);
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
