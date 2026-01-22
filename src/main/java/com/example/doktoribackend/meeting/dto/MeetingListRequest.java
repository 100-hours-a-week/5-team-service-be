package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Getter
public class MeetingListRequest {

    private static final int DEFAULT_SIZE = 10;
    private static final Set<LocalTime> ALLOWED_START_TIME_FROM = Set.of(
            LocalTime.of(9, 0),
            LocalTime.of(14, 0),
            LocalTime.of(19, 0)
    );

    @Min(1)
    @Max(10)
    private Integer size;

    @Min(1)
    private Long cursorId;

    private Integer roundCount;

    private List<MeetingDayOfWeek> dayOfWeek;

    @DateTimeFormat(pattern = "HH:mm")
    private List<LocalTime> startTimeFrom;

    @Min(1)
    private Long readingGenreId;

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }

    @AssertTrue(message = "roundCount must be 1, 3, or 5")
    public boolean isValidRoundCount() {
        if (roundCount == null) {
            return true;
        }
        return roundCount == 1 || roundCount == 3 || roundCount == 5;
    }

    @AssertTrue(message = "startTimeFrom must be one of 09:00, 14:00, 19:00")
    public boolean isValidStartTimeFrom() {
        if (startTimeFrom == null || startTimeFrom.isEmpty()) {
            return true;
        }
        return startTimeFrom.stream().allMatch(ALLOWED_START_TIME_FROM::contains);
    }
}
