package com.example.doktoribackend.user.repository;

import com.example.doktoribackend.user.policy.ReadingPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReadingPurposeRepository extends JpaRepository<ReadingPurpose, Long> {
    List<ReadingPurpose> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    List<ReadingPurpose> findAllByDeletedAtIsNullOrderByPriorityAsc();
}
