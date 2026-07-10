package com.onmom.pregnancy.repository;

import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PregnancyRepository extends JpaRepository<Pregnancy, Long> {

    boolean existsByMotherUserIdAndStatus(Long motherUserId, PregnancyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pregnancy from Pregnancy pregnancy where pregnancy.id = :pregnancyId")
    Optional<Pregnancy> findByIdForUpdate(@Param("pregnancyId") Long pregnancyId);
}
