package com.onmom.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "지원하지 않는 사용자 역할입니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "Authorization 헤더 형식이 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    KAKAO_AUTHORIZATION_CODE_INVALID(HttpStatus.UNAUTHORIZED, "카카오 인가 코드가 올바르지 않습니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "카카오 로그인 처리에 실패했습니다."),
    KAKAO_OAUTH_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 OAuth 설정이 구성되지 않았습니다."),
    KAKAO_OAUTH_CONFIGURATION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 OAuth 설정이 올바르지 않습니다."),
    JWT_SECRET_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 서명 키가 설정되지 않았습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ROLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 사용자 역할로 수행할 수 없는 요청입니다."),
    ACTIVE_PREGNANCY_ALREADY_EXISTS(HttpStatus.CONFLICT, "활성 임신 프로필이 이미 존재합니다."),
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
