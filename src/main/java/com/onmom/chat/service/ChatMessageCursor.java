package com.onmom.chat.service;

import java.time.LocalDateTime;

record ChatMessageCursor(
        LocalDateTime createdAt,
        Long id
) {
}
