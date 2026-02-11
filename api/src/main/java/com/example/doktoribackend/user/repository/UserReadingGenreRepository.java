package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.preference.UserReadingGenre;
import com.example.doktoribackend.user.domain.id.UserReadingGenreId;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserReadingGenreRepository extends JpaRepository<UserReadingGenre, UserReadingGenreId> {
    void deleteByUser(User user);
}
