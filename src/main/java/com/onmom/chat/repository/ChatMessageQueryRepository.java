package com.onmom.chat.repository;

import com.onmom.chat.domain.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageQueryRepository {

    private final EntityManager entityManager;

    public ChatMessageQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<ChatMessage> findBySessionId(
            Long sessionId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        String jpql = """
                select message
                from ChatMessage message
                where message.sessionId = :sessionId
                """;

        if (cursorCreatedAt != null && cursorId != null) {
            jpql += """
                    and (
                        message.createdAt < :cursorCreatedAt
                        or (message.createdAt = :cursorCreatedAt and message.id < :cursorId)
                    )
                    """;
        }

        jpql += "order by message.createdAt desc, message.id desc";

        TypedQuery<ChatMessage> query = entityManager.createQuery(jpql, ChatMessage.class)
                .setParameter("sessionId", sessionId)
                .setMaxResults(limit);

        if (cursorCreatedAt != null && cursorId != null) {
            query.setParameter("cursorCreatedAt", cursorCreatedAt);
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }
}
