package com.onmom.chat.service;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ChatMessageCursorCodec {

    private static final Pattern CREATED_AT_PATTERN = Pattern.compile("\"createdAt\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    ChatMessageCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String createdAt = findRequired(CREATED_AT_PATTERN, json);
            String id = findRequired(ID_PATTERN, json);
            return new ChatMessageCursor(LocalDateTime.parse(createdAt), Long.valueOf(id));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
    }

    String encode(LocalDateTime createdAt, Long id) {
        String json = """
                {"createdAt":"%s","id":%d}
                """.formatted(createdAt, id).trim();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String findRequired(Pattern pattern, String json) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
        return matcher.group(1);
    }
}
