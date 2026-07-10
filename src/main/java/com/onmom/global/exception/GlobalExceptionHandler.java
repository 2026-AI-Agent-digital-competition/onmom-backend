package com.onmom.global.exception;

import com.onmom.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.<Void>error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorResponse>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getStatus())
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        ErrorCode.VALIDATION_FAILED.getMessage(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorResponse>>> handleConstraintViolationException(
            ConstraintViolationException e
    ) {
        List<FieldErrorResponse> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getStatus())
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        ErrorCode.VALIDATION_FAILED.getMessage(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException() {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST_BODY.getStatus())
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.<Void>error(ErrorCode.INVALID_REQUEST_BODY.getCode(), ErrorCode.INVALID_REQUEST_BODY.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException() {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .contentType(APPLICATION_JSON_UTF8)
                .body(ApiResponse.<Void>error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
