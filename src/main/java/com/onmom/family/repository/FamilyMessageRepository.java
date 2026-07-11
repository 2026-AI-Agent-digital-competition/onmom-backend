package com.onmom.family.repository;

import com.onmom.family.domain.FamilyMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMessageRepository extends JpaRepository<FamilyMessage, Long> {
}
