package com.onmom.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void fromReturnsMotherRole() {
        assertThat(UserRole.from("MOTHER")).isEqualTo(UserRole.MOTHER);
    }

    @Test
    void fromReturnsFamilyRoleIgnoringCase() {
        assertThat(UserRole.from("family")).isEqualTo(UserRole.FAMILY);
    }

    @Test
    void fromThrowsExceptionWhenRoleIsInvalid() {
        assertThatThrownBy(() -> UserRole.from("INVALID"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROLE);
    }
}
