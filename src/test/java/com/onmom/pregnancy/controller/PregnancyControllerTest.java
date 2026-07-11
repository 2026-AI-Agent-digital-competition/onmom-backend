package com.onmom.pregnancy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onmom.global.response.ApiResponse;
import com.onmom.pregnancy.dto.CreatePregnancyRequest;
import com.onmom.pregnancy.dto.PregnancyResponse;
import com.onmom.pregnancy.service.PregnancyService;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PregnancyControllerTest {

    @Test
    void createReturnsCreatedResponse() {
        PregnancyService service = mock(PregnancyService.class);
        PregnancyController controller = new PregnancyController(service);
        CreatePregnancyRequest request = new CreatePregnancyRequest("온맘", "튼튼이", 12, 13, null);
        PregnancyResponse pregnancy = new PregnancyResponse(10L, "온맘", "튼튼이", 12, 13, null, "ACTIVE");
        when(service.create(1L, request)).thenReturn(pregnancy);

        ResponseEntity<ApiResponse<PregnancyResponse>> response = controller.create(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(response.getBody()).getData()).isEqualTo(pregnancy);
    }
}
