package com.onmom.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFamilyInsightRequest(
        @NotNull Long pregnancyId,
        Long recipientUserId,
        @NotBlank(message = "sourceText는 필수입니다.")
        @Size(max = 2000, message = "sourceText는 2000자 이하여야 합니다.")
        String sourceText
) {
}
