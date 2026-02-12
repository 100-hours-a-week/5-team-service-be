package com.example.doktoribackend.quiz.domain;

import com.example.doktoribackend.room.domain.ChattingRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizChoiceTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        quiz = Quiz.builder()
                .chattingRoom(room)
                .question("질문")
                .correctChoiceNumber(1)
                .build();
    }

    @Test
    @DisplayName("Builder로 QuizChoice를 생성한다")
    void create() {
        QuizChoice choice = QuizChoice.builder()
                .roomId(1L)
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지 1")
                .build();

        assertThat(choice.getRoomId()).isEqualTo(1L);
        assertThat(choice.getQuiz()).isEqualTo(quiz);
        assertThat(choice.getChoiceNumber()).isEqualTo(1);
        assertThat(choice.getChoiceText()).isEqualTo("선택지 1");
    }

    @Test
    @DisplayName("QuizChoice 생성 시 Quiz의 choices 리스트에 자동으로 추가된다")
    void addedToQuizChoices() {
        QuizChoice choice1 = QuizChoice.builder()
                .roomId(1L)
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지 1")
                .build();

        QuizChoice choice2 = QuizChoice.builder()
                .roomId(1L)
                .quiz(quiz)
                .choiceNumber(2)
                .choiceText("선택지 2")
                .build();

        assertThat(quiz.getChoices()).hasSize(2).containsExactly(choice1, choice2);
    }

    @Test
    @DisplayName("roomId를 직접 파라미터로 받아 설정한다")
    void roomIdSetDirectly() {
        QuizChoice choice = QuizChoice.builder()
                .roomId(99L)
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지")
                .build();

        assertThat(choice.getRoomId()).isEqualTo(99L);
    }
}
