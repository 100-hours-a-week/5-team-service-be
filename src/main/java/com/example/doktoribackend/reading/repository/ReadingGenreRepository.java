package com.example.doktoribackend.reading.repository;

import com.example.doktoribackend.reading.domain.ReadingGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingGenreRepository extends JpaRepository<ReadingGenre, Long> {
    boolean existsByIdAndDeletedAtIsNull(Long id);
}
