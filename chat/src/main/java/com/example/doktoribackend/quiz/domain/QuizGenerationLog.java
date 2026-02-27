package com.example.doktoribackend.quiz.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_generation_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static QuizGenerationLog of(Long userId) {
        QuizGenerationLog log = new QuizGenerationLog();
        log.userId = userId;
        return log;
    }
}
