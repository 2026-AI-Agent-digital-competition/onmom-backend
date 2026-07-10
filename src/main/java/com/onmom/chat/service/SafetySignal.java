package com.onmom.chat.service;

record SafetySignal(
        String alertType,
        String severity,
        String title,
        String description,
        String recommendation
) {
}
