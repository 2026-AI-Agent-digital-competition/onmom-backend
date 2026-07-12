package com.onmom.family.dto;

import java.util.List;

public record FamilyInsightResponse(
        Long translationId,
        String sourceSummary,
        String aiInterpretation,
        String suggestedMessage,
        List<Long> familyMessageIds,
        List<Long> notificationIds
) {
}
