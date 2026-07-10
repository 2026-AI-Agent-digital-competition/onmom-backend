package com.onmom.pregnancy.repository;

import com.onmom.pregnancy.domain.Pregnancy;
import com.onmom.pregnancy.domain.PregnancyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PregnancyRepository extends JpaRepository<Pregnancy, Long> {

    boolean existsByMotherUserIdAndStatus(Long motherUserId, PregnancyStatus status);
}
