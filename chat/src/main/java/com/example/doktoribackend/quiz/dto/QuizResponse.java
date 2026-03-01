package com.example.doktoribackend.quiz.dto;

import com.example.doktoribackend.quiz.domain.Quiz;

import java.util.Comparator;
import java.util.List;

public record QuizResponse(
        String question,
        List<QuizChoiceItem> choices
) {
    public record QuizChoiceItem(Integer choiceNumber, String choiceText) {}

    public static QuizResponse from(Quiz quiz) {
        List<QuizChoiceItem> items = quiz.getChoices().stream()
                .map(c -> new QuizChoiceItem(c.getChoiceNumber(), c.getChoiceText()))
                .sorted(Comparator.comparing(QuizChoiceItem::choiceNumber))
                .toList();
        return new QuizResponse(quiz.getQuestion(), items);
    }
}
