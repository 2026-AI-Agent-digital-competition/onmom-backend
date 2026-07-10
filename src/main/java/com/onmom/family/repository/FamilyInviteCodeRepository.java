package com.onmom.family.repository;

import com.onmom.family.domain.FamilyInviteCode;
import com.onmom.family.domain.FamilyInviteCodeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyInviteCodeRepository extends JpaRepository<FamilyInviteCode, Long> {

    Optional<FamilyInviteCode> findByCode(String code);

    List<FamilyInviteCode> findByPregnancyIdAndStatus(Long pregnancyId, FamilyInviteCodeStatus status);
}
