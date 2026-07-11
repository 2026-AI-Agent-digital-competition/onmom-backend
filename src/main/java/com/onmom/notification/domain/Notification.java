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
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pregnancy_id")
    private Long pregnancyId;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "ref_table", length = 60)
    private String refTable;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(Long userId, Long pregnancyId, String title, String body, Long familyMessageId) {
        this.userId = userId;
        this.pregnancyId = pregnancyId;
        this.notificationType = "FAMILY_AI_INSIGHT";
        this.title = title;
        this.body = body;
        this.priority = "NORMAL";
        this.refTable = "family_messages";
        this.refId = familyMessageId;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }
}
