package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
}

