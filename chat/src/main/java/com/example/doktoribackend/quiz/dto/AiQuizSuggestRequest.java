package com.example.doktoribackend.quiz.dto;

import jakarta.validation.constraints.NotBlank;

public record AiQuizSuggestRequest(
        @NotBlank String author,
        @NotBlank String title
) {}
