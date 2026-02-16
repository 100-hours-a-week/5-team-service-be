package com.example.doktoribackend.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class QuizChoiceId implements Serializable {

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "choice_number", columnDefinition = "TINYINT")
    private Integer choiceNumber;
}
