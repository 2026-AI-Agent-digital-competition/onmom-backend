package com.onmom.pregnancy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pregnancies")
public class Pregnancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long motherUserId;

    @Column(nullable = false, length = 80)
    private String motherDisplayName;

    @Column(length = 80)
    private String babyNickname;

    private Integer pregnancyWeekStart;

    private Integer pregnancyWeekEnd;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PregnancyStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Pregnancy() {
    }

    private Pregnancy(
            Long motherUserId,
            String motherDisplayName,
            String babyNickname,
            Integer pregnancyWeekStart,
            Integer pregnancyWeekEnd,
            LocalDate dueDate
    ) {
        this.motherUserId = motherUserId;
        this.motherDisplayName = motherDisplayName;
        this.babyNickname = babyNickname;
        this.pregnancyWeekStart = pregnancyWeekStart;
        this.pregnancyWeekEnd = pregnancyWeekEnd;
        this.dueDate = dueDate;
        this.status = PregnancyStatus.ACTIVE;
    }

    public static Pregnancy create(
            Long motherUserId,
            String motherDisplayName,
            String babyNickname,
            Integer pregnancyWeekStart,
            Integer pregnancyWeekEnd,
            LocalDate dueDate
    ) {
        return new Pregnancy(
                motherUserId,
                motherDisplayName,
                babyNickname,
                pregnancyWeekStart,
                pregnancyWeekEnd,
                dueDate
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMotherUserId() {
        return motherUserId;
    }

    public String getMotherDisplayName() {
        return motherDisplayName;
    }

    public String getBabyNickname() {
        return babyNickname;
    }

    public Integer getPregnancyWeekStart() {
        return pregnancyWeekStart;
    }

    public Integer getPregnancyWeekEnd() {
        return pregnancyWeekEnd;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public PregnancyStatus getStatus() {
        return status;
    }

    public boolean isMother(Long userId) {
        return motherUserId.equals(userId);
    }
}
