package com.onmom.emotion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "emotion_records")
public class EmotionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "mood_score", nullable = false)
    private Integer moodScore;

    @Column(name = "mood_label", nullable = false, length = 40)
    private String moodLabel;

    @Column(name = "note_text", columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EmotionRecord() {
    }

    public EmotionRecord(
            Long pregnancyId,
            Long userId,
            LocalDate recordDate,
            Integer moodScore,
            String moodLabel,
            String noteText,
            String source
    ) {
        this.pregnancyId = pregnancyId;
        this.userId = userId;
        this.recordDate = recordDate;
        update(userId, moodScore, moodLabel, noteText, source);
    }

    public void update(Long userId, Integer moodScore, String moodLabel, String noteText, String source) {
        this.userId = userId;
        this.moodScore = moodScore;
        this.moodLabel = moodLabel;
        this.noteText = noteText;
        this.source = source == null || source.isBlank() ? "MANUAL" : source;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPregnancyId() {
        return pregnancyId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public Integer getMoodScore() {
        return moodScore;
    }

    public String getMoodLabel() {
        return moodLabel;
    }

    public String getNoteText() {
        return noteText;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
