package com.example.doktoribackend.quiz.repository;

import com.example.doktoribackend.quiz.domain.QuizGenerationLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuizGenerationLogRepository extends JpaRepository<QuizGenerationLog, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM QuizGenerationLog l WHERE l.userId = :userId AND l.createdAt >= :start AND l.createdAt < :end")
    List<QuizGenerationLog> findTodayByUserIdWithLock(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
