package com.onmom.pregnancy.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePregnancyRequest(
        @NotBlank(message = "motherDisplayName은 필수입니다.")
        @Size(max = 80, message = "motherDisplayName은 80자 이하여야 합니다.")
        String motherDisplayName,

        @Size(max = 80, message = "babyNickname은 80자 이하여야 합니다.")
        String babyNickname,

        @Min(value = 0, message = "pregnancyWeekStart는 0 이상이어야 합니다.")
        @Max(value = 42, message = "pregnancyWeekStart는 42 이하여야 합니다.")
        Integer pregnancyWeekStart,

        @Min(value = 0, message = "pregnancyWeekEnd는 0 이상이어야 합니다.")
        @Max(value = 42, message = "pregnancyWeekEnd는 42 이하여야 합니다.")
        Integer pregnancyWeekEnd,

        LocalDate dueDate
) {

    @AssertTrue(message = "임신 주차 시작과 끝은 함께 입력하고 시작 주차가 끝 주차보다 클 수 없습니다.")
    public boolean isPregnancyWeekRangeValid() {
        if (pregnancyWeekStart == null && pregnancyWeekEnd == null) {
            return true;
        }
        return pregnancyWeekStart != null
                && pregnancyWeekEnd != null
                && pregnancyWeekStart <= pregnancyWeekEnd;
    }
}
