package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class MeetingSearchRequest {

    private static final int DEFAULT_SIZE = 10;

    @NotBlank(message = "검색어를 입력해주세요.")
    @Size(min = 1, max = 50, message = "검색어는 1~50자 이내여야 합니다.")
    private String keyword;

    @Min(1)
    @Max(10)
    private Integer size;

    @Min(1)
    private Long cursorId;

    private String readingGenre;

    private List<MeetingDayOfWeek> dayOfWeek;

    private List<StartTimeCode> startTimeFrom;

    private RoundCountCode roundCount;

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }

    public String getKeywordTrimmed() {
        return keyword != null ? keyword.trim() : null;
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