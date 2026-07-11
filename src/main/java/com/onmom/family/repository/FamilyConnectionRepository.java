package com.onmom.family.repository;

import com.onmom.family.domain.FamilyConnection;
import com.onmom.family.domain.FamilyConnectionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyConnectionRepository extends JpaRepository<FamilyConnection, Long> {

    Optional<FamilyConnection> findByPregnancyIdAndFamilyUserId(Long pregnancyId, Long familyUserId);

    boolean existsByPregnancyIdAndFamilyUserIdAndStatus(
            Long pregnancyId,
            Long familyUserId,
            FamilyConnectionStatus status
    );
}
