package com.onmom.pregnancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pregnancies")
public class Pregnancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mother_user_id", nullable = false)
    private Long motherUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected Pregnancy() {
    }

    public Long getId() {
        return id;
    }

    public Long getMotherUserId() {
        return motherUserId;
    }

    public String getStatus() {
        return status;
    }
}
