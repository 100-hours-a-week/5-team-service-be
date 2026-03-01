package com.example.doktoribackend.quiz.dto;

import com.example.doktoribackend.quiz.client.AiQuizGenerateResponse;

import java.util.List;

public record AiQuizSuggestResponse(
        String question,
        Integer correctChoiceNumber,
        List<ChoiceItem> choices
) {
    public record ChoiceItem(Integer choiceNumber, String choiceText) {}

    public static AiQuizSuggestResponse from(AiQuizGenerateResponse response) {
        List<ChoiceItem> choices = response.quizChoices().stream()
                .map(c -> new ChoiceItem(c.choiceNumber(), c.choiceText()))
                .toList();
        return new AiQuizSuggestResponse(response.quiz().question(), response.quiz().correctChoiceNumber(), choices);
    }
}
