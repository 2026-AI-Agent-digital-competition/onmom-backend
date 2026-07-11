package com.onmom.emotion.repository;

import com.onmom.emotion.domain.EmotionTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionTranslationRepository extends JpaRepository<EmotionTranslation, Long> {
}
