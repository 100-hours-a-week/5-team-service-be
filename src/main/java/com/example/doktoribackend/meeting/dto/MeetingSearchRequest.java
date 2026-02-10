package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingDayOfWeek;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Parameter(description = "검색어 (모임 제목 또는 책 제목)", example = "독서", required = true)
    @NotBlank(message = "검색어를 입력해주세요.")
    @Size(min = 1, max = 50, message = "검색어는 1~50자 이내여야 합니다.")
    private String keyword;

    @Parameter(description = "페이지 크기 (1~10)", example = "10")
    @Min(1)
    @Max(10)
    private Integer size;

    @Parameter(description = "커서 ID (페이징용)", example = "100")
    @Min(1)
    private Long cursorId;

    @Parameter(description = "독서 장르 필터 (복수 선택 가능)", schema = @Schema(type = "array",
            allowableValues = {"NOVEL", "ECONOMY_BUSINESS", "ESSAY", "HUMANITIES_PHIL", "SOCIETY_POLITICS", "SELF_DEVELOPMENT", "SCIENCE_TECH", "HISTORY"}))
    private List<String> readingGenres;

    @Parameter(description = "요일 필터 (복수 선택 가능)", schema = @Schema(type = "array",
            allowableValues = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"}))
    private List<MeetingDayOfWeek> dayOfWeek;

    @Parameter(description = "시작 시간대 필터 (복수 선택 가능)", schema = @Schema(type = "array",
            allowableValues = {"MORNING", "AFTERNOON", "EVENING"},
            description = "MORNING: 09:00~14:00, AFTERNOON: 14:00~19:00, EVENING: 19:00~"))
    private List<StartTimeCode> startTimeFrom;

    @Parameter(description = "회차 수 필터", schema = @Schema(allowableValues = {"ONE", "THREE_OR_MORE", "FIVE_OR_MORE"},
            description = "ONE: 1회, THREE_OR_MORE: 3~4회, FIVE_OR_MORE: 5회 이상"))
    private RoundCountCode roundCount;

    @Parameter(hidden = true)
    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }

    @Parameter(hidden = true)
    public String getKeywordTrimmed() {
        return keyword != null ? keyword.trim() : null;
    }

    @Parameter(hidden = true)
    public Integer getRoundCountValue() {
        return roundCount != null ? roundCount.getValue() : null;
    }

    @Parameter(hidden = true)
    public List<LocalTime> getStartTimeValues() {
        if (startTimeFrom == null || startTimeFrom.isEmpty()) {
            return null;
        }
        return startTimeFrom.stream()
                .map(StartTimeCode::getTime)
                .collect(Collectors.toList());
    }
}