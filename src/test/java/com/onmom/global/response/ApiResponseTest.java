package com.onmom.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successReturnsSuccessResultAndData() {
        ApiResponse<Long> response = ApiResponse.success(1L);

        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isEqualTo(1L);
        assertThat(response.getCode()).isNull();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void errorReturnsErrorResultAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("INVALID_ROLE", "지원하지 않는 사용자 역할입니다.");

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
        assertThat(response.getData()).isNull();
        assertThat(response.getCode()).isEqualTo("INVALID_ROLE");
        assertThat(response.getMessage()).isEqualTo("지원하지 않는 사용자 역할입니다.");
    }
}
