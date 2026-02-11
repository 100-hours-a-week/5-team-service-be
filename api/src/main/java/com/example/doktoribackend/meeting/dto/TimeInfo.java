package com.example.doktoribackend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@Builder
public class TimeInfo {
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public static TimeInfo from(LocalTime startTime, Integer durationMinutes) {
        return TimeInfo.builder()
                .startTime(startTime)
                .endTime(startTime.plusMinutes(durationMinutes))
                .build();
    }
}