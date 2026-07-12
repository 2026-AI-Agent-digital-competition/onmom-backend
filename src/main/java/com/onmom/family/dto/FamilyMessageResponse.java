package com.onmom.family.dto;

import com.onmom.family.domain.FamilyMessage;
import java.time.LocalDateTime;

public record FamilyMessageResponse(
        Long id,
        Long pregnancyId,
        Long senderUserId,
        Long recipientUserId,
        Long translationId,
        String messageType,
        String content,
        String status,
        LocalDateTime createdAt
) {

    public static FamilyMessageResponse from(FamilyMessage message) {
        return new FamilyMessageResponse(
                message.getId(),
                message.getPregnancyId(),
                message.getSenderUserId(),
                message.getRecipientUserId(),
                message.getTranslationId(),
                message.getMessageType(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
