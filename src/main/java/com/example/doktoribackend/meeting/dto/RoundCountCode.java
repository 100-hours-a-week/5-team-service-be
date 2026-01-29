package com.example.doktoribackend.meeting.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoundCountCode {
    ONE(1),
    THREE_OR_MORE(3),
    FIVE_OR_MORE(5);

    private final int value;

    public static RoundCountCode fromValue(int value) {
        for (RoundCountCode code : values()) {
            if (code.value == value) {
                return code;
            }
        }
        throw new IllegalArgumentException("Invalid roundCount value: " + value);
    }
}