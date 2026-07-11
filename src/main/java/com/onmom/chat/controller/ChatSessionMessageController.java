package com.onmom.chat.controller;

import com.onmom.chat.dto.ChatMessageListResponse;
import com.onmom.chat.service.ChatService;
import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chat-sessions/{chatSessionId}/messages")
public class ChatSessionMessageController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final ChatService chatService;

    public ChatSessionMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<ChatMessageListResponse>> findMessages(
            @CurrentUserId Long currentUserId,
            @PathVariable Long chatSessionId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size
    ) {
        ChatMessageListResponse response = chatService.findMessages(currentUserId, chatSessionId, cursor, size);
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
