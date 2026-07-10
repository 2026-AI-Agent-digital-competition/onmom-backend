package com.onmom.global.response;

public record ApiResponse<T>(
        ResultType result,
        T data,
        String code,
        String message
) {

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(ResultType.ERROR, null, code, message);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(ResultType.ERROR, data, code, message);
    }
}
