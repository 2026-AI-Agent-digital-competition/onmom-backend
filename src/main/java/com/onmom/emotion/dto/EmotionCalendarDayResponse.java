package com.onmom.emotion.dto;

import com.onmom.emotion.domain.EmotionRecord;
import java.time.LocalDate;

public record EmotionCalendarDayResponse(
        Long emotionRecordId,
        LocalDate recordDate,
        Integer moodScore,
        String moodLabel,
        String moodEmoji,
        boolean hasAiReport,
        Long aiReportId
) {

    public static EmotionCalendarDayResponse of(EmotionRecord record, Long aiReportId) {
        return new EmotionCalendarDayResponse(
                record.getId(),
                record.getRecordDate(),
                record.getMoodScore(),
                record.getMoodLabel(),
                EmotionRecordResponse.moodEmoji(record.getMoodScore(), record.getMoodLabel()),
                aiReportId != null,
                aiReportId
        );
    }
}
