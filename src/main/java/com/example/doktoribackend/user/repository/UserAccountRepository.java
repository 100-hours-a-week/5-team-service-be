package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import com.example.doktoribackend.user.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
