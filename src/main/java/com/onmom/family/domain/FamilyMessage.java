package com.onmom.family.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "family_messages")
public class FamilyMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "translation_id")
    private Long translationId;

    @Column(name = "message_type", nullable = false, length = 40)
    private String messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FamilyMessage() {
    }

    public FamilyMessage(
            Long pregnancyId,
            Long senderUserId,
            Long recipientUserId,
            Long translationId,
            String content
    ) {
        this.pregnancyId = pregnancyId;
        this.senderUserId = senderUserId;
        this.recipientUserId = recipientUserId;
        this.translationId = translationId;
        this.messageType = "AI_INSIGHT";
        this.content = content;
        this.status = "CREATED";
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPregnancyId() {
        return pregnancyId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public Long getTranslationId() {
        return translationId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
