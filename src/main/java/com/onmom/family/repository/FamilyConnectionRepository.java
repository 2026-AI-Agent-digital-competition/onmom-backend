package com.onmom.family.repository;

import com.onmom.family.domain.FamilyConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyConnectionRepository extends JpaRepository<FamilyConnection, Long> {

    List<FamilyConnection> findByPregnancyIdAndMotherUserIdAndStatus(
            Long pregnancyId,
            Long motherUserId,
            String status
    );
}
