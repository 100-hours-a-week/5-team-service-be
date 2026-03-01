package com.example.doktoribackend.quiz.domain;

import com.example.doktoribackend.room.domain.ChattingRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizTest {

    private ChattingRoom createRoom() {
        return ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();
    }

    @Test
    @DisplayName("Quiz 생성 시 ChattingRoom과 연결된다")
    void create() {
        ChattingRoom room = createRoom();

        Quiz quiz = Quiz.builder()
                .chattingRoom(room)
                .question("AI가 인간을 대체할 수 있을까?")
                .correctChoiceNumber(2)
                .build();

        assertThat(quiz.getQuestion()).isEqualTo("AI가 인간을 대체할 수 있을까?");
        assertThat(quiz.getCorrectChoiceNumber()).isEqualTo(2);
        assertThat(quiz.getChattingRoom()).isEqualTo(room);
    }

    @Test
    @DisplayName("정답 번호와 일치하면 isCorrect가 true를 반환한다")
    void isCorrectTrue() {
        ChattingRoom room = createRoom();
        Quiz quiz = Quiz.builder()
                .chattingRoom(room)
                .question("질문")
                .correctChoiceNumber(3)
                .build();

        assertThat(quiz.isCorrect(3)).isTrue();
    }

    @Test
    @DisplayName("정답 번호와 일치하지 않으면 isCorrect가 false를 반환한다")
    void isCorrectFalse() {
        ChattingRoom room = createRoom();
        Quiz quiz = Quiz.builder()
                .chattingRoom(room)
                .question("질문")
                .correctChoiceNumber(3)
                .build();

        assertThat(quiz.isCorrect(1)).isFalse();
    }

    @Test
    @DisplayName("addChoice로 Quiz의 choices에 추가할 수 있다")
    void addChoice() {
        ChattingRoom room = createRoom();
        Quiz quiz = Quiz.builder()
                .chattingRoom(room)
                .question("질문")
                .correctChoiceNumber(1)
                .build();

        QuizChoice choice = QuizChoice.builder()
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지 1")
                .build();
        quiz.addChoice(choice);

        assertThat(quiz.getChoices()).hasSize(1);
        assertThat(quiz.getChoices().getFirst().getChoiceText()).isEqualTo("선택지 1");
    }
}
