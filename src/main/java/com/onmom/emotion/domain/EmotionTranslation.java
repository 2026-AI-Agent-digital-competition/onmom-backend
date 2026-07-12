package com.onmom.emotion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_translations")
public class EmotionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "source_user_id", nullable = false)
    private Long sourceUserId;

    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "ai_interpretation", nullable = false, columnDefinition = "TEXT")
    private String aiInterpretation;

    @Column(name = "suggested_message", nullable = false, columnDefinition = "TEXT")
    private String suggestedMessage;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected EmotionTranslation() {
    }

    public EmotionTranslation(
            Long pregnancyId,
            Long sourceUserId,
            String sourceText,
            String aiInterpretation,
            String suggestedMessage
    ) {
        this.pregnancyId = pregnancyId;
        this.sourceUserId = sourceUserId;
        this.sourceText = sourceText;
        this.aiInterpretation = aiInterpretation;
        this.suggestedMessage = suggestedMessage;
        this.status = "DRAFT";
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAiInterpretation() {
        return aiInterpretation;
    }

    public String getSuggestedMessage() {
        return suggestedMessage;
    }
}
