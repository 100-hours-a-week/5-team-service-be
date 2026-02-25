package com.example.doktoribackend.quiz.repository;

import com.example.doktoribackend.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}
