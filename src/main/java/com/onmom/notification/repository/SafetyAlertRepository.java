package com.onmom.notification.repository;

import com.onmom.notification.domain.SafetyAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyAlertRepository extends JpaRepository<SafetyAlert, Long> {
}
