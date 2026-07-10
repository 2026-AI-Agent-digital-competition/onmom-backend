package com.onmom.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected ChatSession() {
    }

    public ChatSession(Long pregnancyId, Long userId) {
        this.pregnancyId = pregnancyId;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        this.startedAt = LocalDateTime.now();
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }
}
