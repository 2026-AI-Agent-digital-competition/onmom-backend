package com.onmom.pregnancy.dto;

import com.onmom.pregnancy.domain.Pregnancy;
import java.time.LocalDate;

public record PregnancyResponse(
        Long id,
        String motherDisplayName,
        String babyNickname,
        Integer pregnancyWeekStart,
        Integer pregnancyWeekEnd,
        LocalDate dueDate,
        String status
) {

    public static PregnancyResponse from(Pregnancy pregnancy) {
        return new PregnancyResponse(
                pregnancy.getId(),
                pregnancy.getMotherDisplayName(),
                pregnancy.getBabyNickname(),
                pregnancy.getPregnancyWeekStart(),
                pregnancy.getPregnancyWeekEnd(),
                pregnancy.getDueDate(),
                pregnancy.getStatus().name()
        );
    }
}
