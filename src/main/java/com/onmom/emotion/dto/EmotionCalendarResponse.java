package com.onmom.emotion.dto;

import java.util.List;

public record EmotionCalendarResponse(
        Long pregnancyId,
        int year,
        int month,
        List<EmotionCalendarDayResponse> days
) {
}
