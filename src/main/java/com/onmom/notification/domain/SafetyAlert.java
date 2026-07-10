package com.onmom.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "safety_alerts")
public class SafetyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(name = "alert_type", nullable = false, length = 40)
    private String alertType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SafetyAlert() {
    }

    public SafetyAlert(
            Long pregnancyId,
            Long sourceMessageId,
            String alertType,
            String severity,
            String title,
            String description,
            String recommendation
    ) {
        this.pregnancyId = pregnancyId;
        this.sourceMessageId = sourceMessageId;
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
        this.status = "OPEN";
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }
}
