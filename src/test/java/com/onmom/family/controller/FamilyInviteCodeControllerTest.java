package com.onmom.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onmom.family.dto.AcceptFamilyInviteCodeRequest;
import com.onmom.family.dto.AcceptFamilyInviteCodeResponse;
import com.onmom.family.dto.IssueFamilyInviteCodeResponse;
import com.onmom.family.service.FamilyInviteCodeService;
import com.onmom.global.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FamilyInviteCodeControllerTest {

    private final FamilyInviteCodeService service = mock(FamilyInviteCodeService.class);
    private final FamilyInviteCodeController controller = new FamilyInviteCodeController(service);

    @Test
    void issueReturnsSuccessResponse() {
        IssueFamilyInviteCodeResponse invite = new IssueFamilyInviteCodeResponse(
                "ABC234",
                LocalDateTime.of(2026, 7, 11, 0, 10)
        );
        when(service.issue(10L, 1L)).thenReturn(invite);

        ResponseEntity<ApiResponse<IssueFamilyInviteCodeResponse>> response = controller.issue(10L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getData()).isEqualTo(invite);
    }

    @Test
    void acceptReturnsConnectedResponse() {
        AcceptFamilyInviteCodeRequest request = new AcceptFamilyInviteCodeRequest("ABC234");
        AcceptFamilyInviteCodeResponse connection = new AcceptFamilyInviteCodeResponse(1L, 30L, "CONNECTED");
        when(service.accept(20L, request)).thenReturn(connection);

        ResponseEntity<ApiResponse<AcceptFamilyInviteCodeResponse>> response = controller.accept(20L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getBody()).getData()).isEqualTo(connection);
    }
}
