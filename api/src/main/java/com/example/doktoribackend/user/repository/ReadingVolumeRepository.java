package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.policy.ReadingVolume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ReadingVolumeRepository extends JpaRepository<ReadingVolume, Long> {
    Optional<ReadingVolume> findByIdAndDeletedAtIsNull(Long id);

    List<ReadingVolume> findAllByDeletedAtIsNullOrderByPriorityAsc();
}
