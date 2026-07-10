package com.onmom.pregnancy.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CreatePregnancyRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidWeekRange() {
        CreatePregnancyRequest request = new CreatePregnancyRequest("온맘", "튼튼이", 12, 13, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsIncompleteWeekRange() {
        CreatePregnancyRequest request = new CreatePregnancyRequest("온맘", null, 12, null, null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("pregnancyWeekRangeValid"));
    }

    @Test
    void rejectsReversedWeekRange() {
        CreatePregnancyRequest request = new CreatePregnancyRequest("온맘", null, 13, 12, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsWeekOutsideSupportedRange() {
        CreatePregnancyRequest request = new CreatePregnancyRequest("온맘", null, 0, 43, null);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
