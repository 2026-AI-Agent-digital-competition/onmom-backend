package com.onmom.family.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class AcceptFamilyInviteCodeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsLowercaseAllowedCode() {
        assertThat(validator.validate(new AcceptFamilyInviteCodeRequest("abc234"))).isEmpty();
    }

    @Test
    void rejectsAmbiguousCharacters() {
        assertThat(validator.validate(new AcceptFamilyInviteCodeRequest("ABC01I"))).isNotEmpty();
    }

    @Test
    void rejectsWrongLength() {
        assertThat(validator.validate(new AcceptFamilyInviteCodeRequest("ABC23"))).isNotEmpty();
    }
}
