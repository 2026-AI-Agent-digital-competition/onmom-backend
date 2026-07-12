package com.onmom.chat.repository;

import com.onmom.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByIdAndPregnancyIdAndUserId(Long id, Long pregnancyId, Long userId);
}
