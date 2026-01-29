package com.example.doktoribackend.meeting.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;

@Getter
@RequiredArgsConstructor
public enum StartTimeCode {
    MORNING(LocalTime.of(9, 0)),
    AFTERNOON(LocalTime.of(14, 0)),
    EVENING(LocalTime.of(19, 0));

    private final LocalTime time;

    public static StartTimeCode fromTime(LocalTime time) {
        for (StartTimeCode code : values()) {
            if (code.time.equals(time)) {
                return code;
            }
        }
        throw new IllegalArgumentException("Invalid startTime value: " + time);
    }
}