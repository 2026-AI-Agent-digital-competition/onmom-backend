package com.onmom.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_reports")
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "emotion_record_id")
    private Long emotionRecordId;

    @Column(name = "report_type", nullable = false, length = 40)
    private String reportType;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model_name", length = 80)
    private String modelName;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    protected AiReport() {
    }

    public AiReport(Long pregnancyId, String reportType, String title, String content, String modelName) {
        this.pregnancyId = pregnancyId;
        this.reportType = reportType;
        this.title = title;
        this.content = content;
        this.modelName = modelName;
    }

    @PrePersist
    void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }
}
