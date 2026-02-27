package com.example.doktoribackend.quiz.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiQuizGenerateResponse(
        String question,
        @JsonProperty("correct_choice_number") Integer correctChoiceNumber,
        @JsonProperty("room_id") Long roomId,
        List<ChoiceItem> choices
) {
    public record ChoiceItem(
            @JsonProperty("choice_number") Integer choiceNumber,
            @JsonProperty("choice_text") String choiceText
    ) {}
}
