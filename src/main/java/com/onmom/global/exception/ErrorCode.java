package com.onmom.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 형식이 올바르지 않습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "지원하지 않는 사용자 역할입니다."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "Authorization 헤더 형식이 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    KAKAO_AUTHORIZATION_CODE_INVALID(HttpStatus.UNAUTHORIZED, "카카오 인가 코드가 올바르지 않습니다."),

    PREGNANCY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "임신 프로필에 접근할 권한이 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PREGNANCY_NOT_FOUND(HttpStatus.NOT_FOUND, "임신 프로필을 찾을 수 없습니다."),
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."),
    EMOTION_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "감정 기록을 찾을 수 없습니다."),
    AI_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 리포트를 찾을 수 없습니다."),
    CONNECTED_FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "연결된 가족을 찾을 수 없습니다."),

    KAKAO_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "카카오 로그인 처리에 실패했습니다."),
    AI_MESSAGE_FAILED(HttpStatus.BAD_GATEWAY, "AI 응답 생성에 실패했습니다."),

    KAKAO_OAUTH_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 OAuth 설정이 구성되지 않았습니다."),
    KAKAO_OAUTH_CONFIGURATION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 OAuth 설정이 올바르지 않습니다."),
    JWT_SECRET_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 서명 키가 설정되지 않았습니다."),
    AI_API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 설정이 완료되지 않았습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
