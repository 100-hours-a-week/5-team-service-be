package com.example.doktoribackend.reading.repository;

import com.example.doktoribackend.reading.domain.ReadingGenre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReadingGenreRepository extends JpaRepository<ReadingGenre, Long> {
    boolean existsByIdAndDeletedAtIsNull(Long id);

    List<ReadingGenre> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    List<ReadingGenre> findAllByDeletedAtIsNullOrderByPriorityAsc();
}
