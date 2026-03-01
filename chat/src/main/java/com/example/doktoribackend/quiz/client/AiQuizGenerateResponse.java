package com.example.doktoribackend.quiz.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiQuizGenerateResponse(
        Quiz quiz,
        @JsonProperty("quiz_choices") List<ChoiceItem> quizChoices
) {
    public record Quiz(
            String question,
            @JsonProperty("correct_choice_number") Integer correctChoiceNumber
    ) {}

    public record ChoiceItem(
            @JsonProperty("choice_number") Integer choiceNumber,
            @JsonProperty("choice_text") String choiceText
    ) {}
}
