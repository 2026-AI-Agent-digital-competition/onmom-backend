package com.onmom.global.exception;

public record FieldErrorResponse(
        String field,
        String message
) {
}
