package com.example.doktoribackend.quiz.repository;

import com.example.doktoribackend.quiz.domain.QuizGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface QuizGenerationLogRepository extends JpaRepository<QuizGenerationLog, Long> {

    @Query("SELECT COUNT(l) FROM QuizGenerationLog l WHERE l.userId = :userId AND l.createdAt >= :start AND l.createdAt < :end")
    int countTodayByUserId(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
