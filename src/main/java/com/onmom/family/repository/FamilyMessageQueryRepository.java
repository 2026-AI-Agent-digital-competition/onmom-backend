package com.onmom.family.repository;

import com.onmom.family.domain.FamilyMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class FamilyMessageQueryRepository {

    private final EntityManager entityManager;

    public FamilyMessageQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<FamilyMessage> findReceivedMessages(
            Long recipientUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    ) {
        String jpql = """
                select message
                from FamilyMessage message
                where message.recipientUserId = :recipientUserId
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

        TypedQuery<FamilyMessage> query = entityManager.createQuery(jpql, FamilyMessage.class)
                .setParameter("recipientUserId", recipientUserId)
                .setMaxResults(limit);

        if (cursorCreatedAt != null && cursorId != null) {
            query.setParameter("cursorCreatedAt", cursorCreatedAt);
            query.setParameter("cursorId", cursorId);
        }

        return query.getResultList();
    }
}
