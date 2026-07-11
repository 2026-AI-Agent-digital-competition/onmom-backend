package com.onmom.chat.dto;

import java.util.List;

public record ChatMessageListResponse(
        Long chatSessionId,
        List<ChatMessageItemResponse> content,
        ChatCursorPageResponse page
) {
}
