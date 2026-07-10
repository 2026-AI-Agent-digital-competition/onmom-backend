package com.onmom.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primary_role", nullable = false, length = 20)
    private String primaryRole;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected UserAccount() {
    }

    public Long getId() {
        return id;
    }

    public String getPrimaryRole() {
        return primaryRole;
    }

    public String getStatus() {
        return status;
    }
}
