package com.onmom.emotion.controller;

import com.onmom.emotion.dto.CreateEmotionRecordRequest;
import com.onmom.emotion.dto.EmotionAiReportResponse;
import com.onmom.emotion.dto.EmotionCalendarResponse;
import com.onmom.emotion.dto.EmotionRecordResponse;
import com.onmom.emotion.service.EmotionRecordService;
import com.onmom.global.auth.CurrentUserId;
import com.onmom.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/emotion-records")
public class EmotionRecordController {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final EmotionRecordService emotionRecordService;

    public EmotionRecordController(EmotionRecordService emotionRecordService) {
        this.emotionRecordService = emotionRecordService;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse<EmotionRecordResponse>> createOrUpdate(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CreateEmotionRecordRequest request
    ) {
        EmotionRecordResponse response = emotionRecordService.createOrUpdate(currentUserId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<EmotionRecordResponse>> findDaily(
            @CurrentUserId Long currentUserId,
            @RequestParam @NotNull Long pregnancyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate
    ) {
        EmotionRecordResponse response = emotionRecordService.findDaily(currentUserId, pregnancyId, recordDate);
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<EmotionCalendarResponse>> findCalendar(
            @CurrentUserId Long currentUserId,
            @RequestParam @NotNull Long pregnancyId,
            @RequestParam @Min(2000) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        EmotionCalendarResponse response = emotionRecordService.findCalendar(
                currentUserId,
                pregnancyId,
                year,
                month
        );
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{emotionRecordId}/ai-report")
    public ResponseEntity<ApiResponse<EmotionAiReportResponse>> findAiReport(
            @CurrentUserId Long currentUserId,
            @PathVariable Long emotionRecordId
    ) {
        EmotionAiReportResponse response = emotionRecordService.findAiReport(currentUserId, emotionRecordId);
        return ResponseEntity
                .ok()
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.success(response));
    }
}
