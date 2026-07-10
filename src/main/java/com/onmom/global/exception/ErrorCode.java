package com.onmom.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "요청 본문이 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    PREGNANCY_NOT_FOUND(HttpStatus.NOT_FOUND, "PREGNANCY_NOT_FOUND", "임신 프로필을 찾을 수 없습니다."),
    PREGNANCY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PREGNANCY_ACCESS_DENIED", "임신 프로필에 접근할 권한이 없습니다."),
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_SESSION_NOT_FOUND", "채팅 세션을 찾을 수 없습니다."),
    AI_API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_API_KEY_NOT_CONFIGURED", "AI 설정이 완료되지 않았습니다."),
    AI_MESSAGE_FAILED(HttpStatus.BAD_GATEWAY, "AI_MESSAGE_FAILED", "AI 응답 생성에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
