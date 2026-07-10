package com.onmom.ai.controller;

import com.onmom.ai.dto.AiMessageResponse;
import com.onmom.ai.dto.CreateAiMessageRequest;
import com.onmom.ai.service.GeminiService;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/ai-messages")
public class AiMessageController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final GeminiService geminiService;

    public AiMessageController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<AiMessageResponse>> create(@Valid @RequestBody CreateAiMessageRequest request) {
        AiMessageResponse response = new AiMessageResponse(geminiService.ask(request.message()));
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
