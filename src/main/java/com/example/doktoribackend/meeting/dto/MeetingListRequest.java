package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class MeetingListRequest {

    private static final int DEFAULT_SIZE = 10;

    @Min(1)
    @Max(10)
    private Integer size;

    @Min(1)
    private Long cursorId;

    private RoundCountCode roundCount;

    private List<MeetingDayOfWeek> dayOfWeek;

    private List<StartTimeCode> startTimeFrom;

    private String readingGenre;

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }

    public Integer getRoundCountValue() {
        return roundCount != null ? roundCount.getValue() : null;
    }
    
    public List<LocalTime> getStartTimeValues() {
        if (startTimeFrom == null || startTimeFrom.isEmpty()) {
            return null;
        }
        return startTimeFrom.stream()
                .map(StartTimeCode::getTime)
                .collect(Collectors.toList());
    }
}
