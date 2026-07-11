package com.onmom.user.domain;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.util.Locale;

public enum UserRole {
    MOTHER,
    FAMILY;

    public static UserRole from(String value) {
        try {
            return UserRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
    }
}
