package com.example.doktoribackend.quiz.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiQuizGenerateRequest(
        String author,
        String title,
        @JsonProperty("room_id") Long roomId
) {}
