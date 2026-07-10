package com.onmom.chat.dto;

import java.util.List;

public record ChatMessageResponse(
        Long chatSessionId,
        Long userMessageId,
        Long aiMessageId,
        String answer,
        String riskLevel,
        List<Long> safetyAlertIds,
        Long aiReportId
) {
}
