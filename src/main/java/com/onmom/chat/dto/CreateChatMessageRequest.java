package com.onmom.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChatMessageRequest(
        @NotNull Long pregnancyId,
        Long chatSessionId,
        @NotBlank(message = "message는 필수입니다.")
        @Size(max = 2000, message = "message는 2000자 이하여야 합니다.")
        String message
) {
}
