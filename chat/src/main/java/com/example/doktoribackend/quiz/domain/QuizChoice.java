package com.example.doktoribackend.quiz.domain;

import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_choices")
@IdClass(QuizChoiceId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizChoice {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @Id
    @Column(name = "choice_number", columnDefinition = "TINYINT")
    private Integer choiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Quiz quiz;

    @Column(name = "choice_text", nullable = false, length = 100)
    private String choiceText;

    public static QuizChoice create(Quiz quiz, ChatRoomCreateRequest.QuizChoiceRequest request) {
        return new QuizChoice(quiz.getRoomId(), quiz, request.choiceNumber(), request.text());
    }

    @Builder
    private QuizChoice(Long roomId, Quiz quiz, Integer choiceNumber, String choiceText) {
        this.roomId = roomId;
        this.quiz = quiz;
        this.choiceNumber = choiceNumber;
        this.choiceText = choiceText;
        quiz.addChoice(this);
    }
}
