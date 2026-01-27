package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class MeetingSearchRequest {

    private static final int DEFAULT_SIZE = 10;
    private static final Set<LocalTime> ALLOWED_START_TIME_FROM = Set.of(
            LocalTime.of(9, 0),
            LocalTime.of(14, 0),
            LocalTime.of(19, 0)
    );

    @NotBlank(message = "검색어를 입력해주세요.")
    @Size(min = 1, max = 50, message = "검색어는 1~50자 이내여야 합니다.")
    private String keyword;

    @Min(1)
    @Max(10)
    private Integer size;

    @Min(1)
    private Long cursorId;

    @Min(1)
    private Long readingGenreId;

    private List<MeetingDayOfWeek> dayOfWeek;

    @DateTimeFormat(pattern = "HH:mm")
    private List<LocalTime> startTimeFrom;

    private Integer roundCount;

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }

    public String getKeywordTrimmed() {
        return keyword != null ? keyword.trim() : null;
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