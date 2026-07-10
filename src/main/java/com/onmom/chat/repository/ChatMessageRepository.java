package com.onmom.chat.repository;

import com.onmom.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop12BySessionIdOrderByCreatedAtDesc(Long sessionId);
}
