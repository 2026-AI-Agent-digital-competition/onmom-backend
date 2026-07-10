package com.onmom.family.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "family_invite_codes",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_invite_code", columnNames = "code")
)
public class FamilyInviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pregnancyId;

    @Column(nullable = false)
    private Long inviterUserId;

    @Column(nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyInviteCodeStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FamilyInviteCode() {
    }

    private FamilyInviteCode(Long pregnancyId, Long inviterUserId, String code, LocalDateTime expiresAt) {
        this.pregnancyId = pregnancyId;
        this.inviterUserId = inviterUserId;
        this.code = code;
        this.status = FamilyInviteCodeStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public static FamilyInviteCode issue(Long pregnancyId, Long inviterUserId, String code, LocalDateTime expiresAt) {
        return new FamilyInviteCode(pregnancyId, inviterUserId, code, expiresAt);
    }

    public Long getPregnancyId() {
        return pregnancyId;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public String getCode() {
        return code;
    }

    public FamilyInviteCodeStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isPending() {
        return status == FamilyInviteCodeStatus.PENDING;
    }

    public boolean hasExpiredStatus() {
        return status == FamilyInviteCodeStatus.EXPIRED;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke() {
        this.status = FamilyInviteCodeStatus.REVOKED;
    }

    public void expire() {
        this.status = FamilyInviteCodeStatus.EXPIRED;
    }
}
