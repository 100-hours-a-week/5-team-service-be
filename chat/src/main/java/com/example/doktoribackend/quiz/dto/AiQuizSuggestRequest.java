package com.example.doktoribackend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiQuizSuggestRequest(
        @NotBlank String author,
        @NotBlank String title,
        @NotNull @JsonProperty("room_id") Long roomId
) {}
