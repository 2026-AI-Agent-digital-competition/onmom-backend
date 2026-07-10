package com.onmom.pregnancy.repository;

import com.onmom.pregnancy.domain.Pregnancy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PregnancyRepository extends JpaRepository<Pregnancy, Long> {
}
