package com.onmom.chat.dto;

public record ChatCursorPageResponse(
        String nextCursor,
        int size,
        boolean hasNext
) {
}
