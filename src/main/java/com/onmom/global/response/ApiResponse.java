package com.onmom.global.response;

public record ApiResponse<T>(
        ResultType result,
        T data,
        String code,
        String message
) {

    public static <S> ApiResponse<S> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null, null);
    }

    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null, null);
    }

    public static <S> ApiResponse<S> error(String code, String message) {
        return new ApiResponse<>(ResultType.ERROR, null, code, message);
    }

    public static <S> ApiResponse<S> error(String code, String message, S data) {
        return new ApiResponse<>(ResultType.ERROR, data, code, message);
    }
}
