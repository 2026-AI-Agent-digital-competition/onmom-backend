package com.onmom.family.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "family_connections")
public class FamilyConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pregnancy_id", nullable = false)
    private Long pregnancyId;

    @Column(name = "mother_user_id", nullable = false)
    private Long motherUserId;

    @Column(name = "family_user_id", nullable = false)
    private Long familyUserId;

    @Column(name = "relationship", nullable = false, length = 20)
    private String relationship;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected FamilyConnection() {
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

    public String getRelationship() {
        return relationship;
    }

    public String getStatus() {
        return status;
    }
}
