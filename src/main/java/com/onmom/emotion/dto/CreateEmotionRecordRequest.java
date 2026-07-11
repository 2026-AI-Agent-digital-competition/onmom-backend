package com.onmom.emotion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateEmotionRecordRequest(
        @NotNull Long pregnancyId,
        @NotNull LocalDate recordDate,
        @NotNull @Min(1) @Max(5) Integer moodScore,
        @NotBlank @Size(max = 40) String moodLabel,
        String noteText,
        @Size(max = 20) String source
) {
}
