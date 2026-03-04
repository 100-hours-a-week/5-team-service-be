package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.user.domain.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByProviderAndProviderIdAndDeletedAtIsNull(OAuthProvider provider, String providerId);
    boolean existsByProviderAndProviderId(OAuthProvider provider, String providerId);

    @EntityGraph(attributePaths = "user")
    List<UserAccount> findAllByProviderAndProviderIdStartingWith(OAuthProvider provider, String prefix);
}
