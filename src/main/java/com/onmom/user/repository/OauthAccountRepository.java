package com.onmom.user.repository;

import com.onmom.user.domain.OauthAccount;
import com.onmom.user.domain.OauthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<OauthAccount> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId);
}
