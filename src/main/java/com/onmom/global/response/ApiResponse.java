package com.onmom.global.response;

public record ApiResponse<T>(
        ResultType result,
        T data,
        String code,
        String message
) {
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final ResultType result;
    private final T data;
    private final String code;
    private final String message;

    public static <S> ApiResponse<S> success() {
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultType.SUCCESS, null, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
    public static <S> ApiResponse<S> success(S data) {
        return new ApiResponse<>(ResultType.SUCCESS, data, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
    public static <S> ApiResponse<S> error(String code, String message) {
        return new ApiResponse<>(ResultType.ERROR, null, code, message);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
    public static <S> ApiResponse<S> error(String code, String message, S data) {
        return new ApiResponse<>(ResultType.ERROR, data, code, message);
    }
}
