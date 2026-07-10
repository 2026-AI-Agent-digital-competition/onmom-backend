package com.onmom.family.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class FamilyInviteCodeGenerator {

    static final int CODE_LENGTH = 6;
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
