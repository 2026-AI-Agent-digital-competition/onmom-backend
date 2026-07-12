package com.onmom.emotion.dto;

import com.onmom.emotion.domain.EmotionRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmotionRecordResponse(
        Long id,
        Long pregnancyId,
        Long userId,
        LocalDate recordDate,
        Integer moodScore,
        String moodLabel,
        String moodEmoji,
        String noteText,
        String source,
        Long aiReportId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EmotionRecordResponse from(EmotionRecord record) {
        return new EmotionRecordResponse(
                record.getId(),
                record.getPregnancyId(),
                record.getUserId(),
                record.getRecordDate(),
                record.getMoodScore(),
                record.getMoodLabel(),
                moodEmoji(record.getMoodScore(), record.getMoodLabel()),
                record.getNoteText(),
                record.getSource(),
                null,
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public static EmotionRecordResponse from(EmotionRecord record, Long aiReportId) {
        return new EmotionRecordResponse(
                record.getId(),
                record.getPregnancyId(),
                record.getUserId(),
                record.getRecordDate(),
                record.getMoodScore(),
                record.getMoodLabel(),
                moodEmoji(record.getMoodScore(), record.getMoodLabel()),
                record.getNoteText(),
                record.getSource(),
                aiReportId,
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public static String moodEmoji(Integer moodScore, String moodLabel) {
        String label = moodLabel == null ? "" : moodLabel;
        if (label.contains("우울") || label.contains("슬픔")) {
            return "😢";
        }
        if (label.contains("화") || label.contains("분노")) {
            return "😡";
        }
        if (label.contains("불안")) {
            return "😟";
        }
        if (label.contains("행복") || label.contains("기쁨")) {
            return "😊";
        }
        if (label.contains("사랑") || label.contains("감사")) {
            return "🥰";
        }

        return switch (moodScore) {
            case 1 -> "😭";
            case 2 -> "😢";
            case 3 -> "😐";
            case 4 -> "🙂";
            case 5 -> "😊";
            default -> "🙂";
        };
    }
}
