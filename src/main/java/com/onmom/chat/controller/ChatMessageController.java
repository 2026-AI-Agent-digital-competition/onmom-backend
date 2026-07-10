package com.onmom.chat.controller;

import com.onmom.chat.dto.ChatMessageResponse;
import com.onmom.chat.dto.CreateChatMessageRequest;
import com.onmom.chat.service.ChatService;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/chat-messages")
public class ChatMessageController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final ChatService chatService;

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> create(
            @Valid @RequestBody CreateChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.createMessage(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
