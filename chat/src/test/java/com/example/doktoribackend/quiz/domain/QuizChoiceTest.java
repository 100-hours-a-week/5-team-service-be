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
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지 1")
                .build();

        assertThat(choice.getQuiz()).isEqualTo(quiz);
        assertThat(choice.getChoiceNumber()).isEqualTo(1);
        assertThat(choice.getChoiceText()).isEqualTo("선택지 1");
    }

    @Test
    @DisplayName("addChoice로 Quiz의 choices 리스트에 추가할 수 있다")
    void addedToQuizChoices() {
        QuizChoice choice1 = QuizChoice.builder()
                .quiz(quiz)
                .choiceNumber(1)
                .choiceText("선택지 1")
                .build();
        quiz.addChoice(choice1);

        QuizChoice choice2 = QuizChoice.builder()
                .quiz(quiz)
                .choiceNumber(2)
                .choiceText("선택지 2")
                .build();
        quiz.addChoice(choice2);

        assertThat(quiz.getChoices()).hasSize(2).containsExactly(choice1, choice2);
    }
}
