package com.example.doktoribackend.auth.repository;

import com.example.doktoribackend.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}

