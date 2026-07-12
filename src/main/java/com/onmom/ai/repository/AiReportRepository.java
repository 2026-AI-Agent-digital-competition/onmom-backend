package com.onmom.ai.repository;

import com.onmom.ai.domain.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findTopByEmotionRecordIdAndReportTypeOrderByGeneratedAtDesc(
            Long emotionRecordId,
            String reportType
    );
}
