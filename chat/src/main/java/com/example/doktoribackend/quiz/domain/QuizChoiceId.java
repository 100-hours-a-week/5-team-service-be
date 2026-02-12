package com.example.doktoribackend.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuizChoiceId implements Serializable {

    private Long roomId;
    private Integer choiceNumber;
}
