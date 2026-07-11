package com.onmom.family.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FamilyInviteCodeGeneratorTest {

    private final FamilyInviteCodeGenerator generator = new FamilyInviteCodeGenerator();

    @Test
    void generateReturnsSixCharacterCode() {
        String code = generator.generate();

        assertThat(code).hasSize(6);
    }

    @Test
    void generateUsesAllowedCharactersOnly() {
        String code = generator.generate();

        assertThat(code).matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$");
    }
}
