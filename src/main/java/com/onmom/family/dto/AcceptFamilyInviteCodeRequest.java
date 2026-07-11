package com.onmom.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcceptFamilyInviteCodeRequest(
        @NotBlank(message = "code는 필수입니다.")
        @Pattern(regexp = "^[A-HJ-NP-Za-hj-np-z2-9]{6}$", message = "code는 6글자 영문/숫자여야 합니다.")
        String code
) {
}
