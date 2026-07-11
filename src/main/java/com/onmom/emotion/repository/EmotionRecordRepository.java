package com.onmom.emotion.repository;

import com.onmom.emotion.domain.EmotionRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, Long> {

    Optional<EmotionRecord> findByPregnancyIdAndRecordDate(Long pregnancyId, LocalDate recordDate);

    List<EmotionRecord> findByPregnancyIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long pregnancyId,
            LocalDate startDate,
            LocalDate endDate
    );
}
