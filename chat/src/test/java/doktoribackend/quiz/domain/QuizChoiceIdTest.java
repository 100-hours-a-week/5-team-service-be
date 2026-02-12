package doktoribackend.quiz.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuizChoiceIdTest {

    @Test
    @DisplayName("같은 roomId와 choiceNumber면 equals가 true를 반환한다")
    void equalsTrue() {
        QuizChoiceId id1 = new QuizChoiceId(1L, 1);
        QuizChoiceId id2 = new QuizChoiceId(1L, 1);

        assertThat(id1).isEqualTo(id2).hasSameHashCodeAs(id2);
    }

    @Test
    @DisplayName("다른 roomId면 equals가 false를 반환한다")
    void equalsFalseDifferentRoomId() {
        QuizChoiceId id1 = new QuizChoiceId(1L, 1);
        QuizChoiceId id2 = new QuizChoiceId(2L, 1);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("다른 choiceNumber면 equals가 false를 반환한다")
    void equalsFalseDifferentChoiceNumber() {
        QuizChoiceId id1 = new QuizChoiceId(1L, 1);
        QuizChoiceId id2 = new QuizChoiceId(1L, 2);

        assertThat(id1).isNotEqualTo(id2);
    }
}
