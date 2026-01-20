package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByKakaoIdAndDeletedAtIsNull(Long kakaoId);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
