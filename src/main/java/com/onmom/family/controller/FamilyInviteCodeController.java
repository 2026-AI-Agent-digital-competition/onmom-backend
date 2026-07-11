package com.onmom.family.controller;

import com.onmom.family.dto.AcceptFamilyInviteCodeRequest;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.family.service.FamilyInviteCodeService;
import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FamilyInviteCodeController {

    private final FamilyInviteCodeService familyInviteCodeService;

    public FamilyInviteCodeController(FamilyInviteCodeService familyInviteCodeService) {
        this.familyInviteCodeService = familyInviteCodeService;
    }

    @PostMapping("/pregnancies/{pregnancyId}/family-invite-codes")
    public ResponseEntity<ApiResponse<IssueFamilyInviteCodeResponse>> issue(
            @CurrentUserId Long currentUserId,
            @PathVariable Long pregnancyId
    ) {
        IssueFamilyInviteCodeResponse response = familyInviteCodeService.issue(currentUserId, pregnancyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/family-invite-codes/accept")
    public ResponseEntity<ApiResponse<AcceptFamilyInviteCodeResponse>> accept(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody AcceptFamilyInviteCodeRequest request
    ) {
        AcceptFamilyInviteCodeResponse response = familyInviteCodeService.accept(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
