package com.example.doktoribackend.quiz.domain;

import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_choices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizChoice {

    @EmbeddedId
    private QuizChoiceId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Quiz quiz;

    @Column(name = "choice_text", nullable = false, length = 100)
    private String choiceText;

    public static QuizChoice create(Quiz quiz, ChatRoomCreateRequest.QuizChoiceRequest request) {
        return new QuizChoice(quiz, request.choiceNumber(), request.text());
    }

    @Builder
    private QuizChoice(Quiz quiz, Integer choiceNumber, String choiceText) {
        this.id = new QuizChoiceId(quiz.getRoomId(), choiceNumber);
        this.quiz = quiz;
        this.choiceText = choiceText;
    }

    public Integer getChoiceNumber() {
        return this.id.getChoiceNumber();
    }
}
