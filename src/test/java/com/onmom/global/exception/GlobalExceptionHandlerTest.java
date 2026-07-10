package com.onmom.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.onmom.global.response.ApiResponse;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedJsonReturnsValidationError() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Malformed JSON",
                mock(HttpInputMessage.class)
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidRequest(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.getMessage()).isEqualTo("요청 값이 올바르지 않습니다.");
    }

    @Test
    void unexpectedExceptionReturnsInternalServerError() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new IllegalStateException("failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
