package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.preference.UserReadingPurpose;
import com.example.doktoribackend.user.domain.id.UserReadingPurposeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReadingPurposeRepository extends JpaRepository<UserReadingPurpose, UserReadingPurposeId> {
    void deleteByUser(User user);
}
