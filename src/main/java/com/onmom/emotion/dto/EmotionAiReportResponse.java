package com.onmom.emotion.dto;

import com.onmom.ai.domain.AiReport;
import java.time.LocalDateTime;

public record EmotionAiReportResponse(
        Long aiReportId,
        Long emotionRecordId,
        String reportType,
        String title,
        String content,
        String modelName,
        LocalDateTime generatedAt
) {

    public static EmotionAiReportResponse from(AiReport report) {
        return new EmotionAiReportResponse(
                report.getId(),
                report.getEmotionRecordId(),
                report.getReportType(),
                report.getTitle(),
                report.getContent(),
                report.getModelName(),
                report.getGeneratedAt()
        );
    }
}
