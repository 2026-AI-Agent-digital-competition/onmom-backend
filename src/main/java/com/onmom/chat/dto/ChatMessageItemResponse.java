package com.onmom.chat.dto;

import com.onmom.chat.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageItemResponse(
        Long id,
        Long chatSessionId,
        String senderType,
        String messageType,
        String content,
        String metadata,
        LocalDateTime createdAt
) {

    public static ChatMessageItemResponse from(ChatMessage message) {
        return new ChatMessageItemResponse(
                message.getId(),
                message.getSessionId(),
                message.getSenderType().name(),
                message.getMessageType(),
                message.getContent(),
                message.getMetadata(),
                message.getCreatedAt()
        );
    }
}
