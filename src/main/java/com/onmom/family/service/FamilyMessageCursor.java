package com.onmom.family.service;

import java.time.LocalDateTime;

record FamilyMessageCursor(
        LocalDateTime createdAt,
        Long id
) {
}
