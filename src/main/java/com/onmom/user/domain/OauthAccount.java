package com.onmom.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "oauth_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_oauth_provider_user", columnNames = {"provider", "provider_user_id"})
)
public class OauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OauthProvider provider;

    @Column(nullable = false, length = 120)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    protected OauthAccount() {
    }

    private OauthAccount(User user, OauthProvider provider, String providerUserId, String email) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }

    public static OauthAccount createKakao(User user, String providerUserId, String email) {
        return new OauthAccount(user, OauthProvider.KAKAO, providerUserId, email);
    }

    public User getUser() {
        return user;
    }
}
