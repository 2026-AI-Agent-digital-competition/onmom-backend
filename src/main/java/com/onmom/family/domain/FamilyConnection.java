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
        name = "family_connections",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_connection", columnNames = {"pregnancy_id", "family_user_id"})
)
public class FamilyConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pregnancyId;

    @Column(nullable = false)
    private Long motherUserId;

    @Column(nullable = false)
    private Long familyUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyRelationship relationship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyConnectionStatus status;

    private LocalDateTime connectedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FamilyConnection() {
    }

    private FamilyConnection(
            Long pregnancyId,
            Long motherUserId,
            Long familyUserId,
            LocalDateTime connectedAt
    ) {
        this.pregnancyId = pregnancyId;
        this.motherUserId = motherUserId;
        this.familyUserId = familyUserId;
        this.relationship = FamilyRelationship.FAMILY;
        this.status = FamilyConnectionStatus.CONNECTED;
        this.connectedAt = connectedAt;
    }

    public static FamilyConnection connect(
            Long pregnancyId,
            Long motherUserId,
            Long familyUserId,
            LocalDateTime connectedAt
    ) {
        return new FamilyConnection(pregnancyId, motherUserId, familyUserId, connectedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getPregnancyId() {
        return pregnancyId;
    }

    public Long getMotherUserId() {
        return motherUserId;
    }

    public Long getFamilyUserId() {
        return familyUserId;
    }

    public FamilyRelationship getRelationship() {
        return relationship;
    }

    public FamilyConnectionStatus getStatus() {
        return status;
    }

    public void connectAgain(LocalDateTime connectedAt) {
        this.status = FamilyConnectionStatus.CONNECTED;
        this.connectedAt = connectedAt;
    }
}
