package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.common.jackson.FlexibleLocalDateDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class MeetingCreateRequest {

    @NotBlank
    @Schema(example = "https://cdn.example.com/meetings/2026/01/10/uuid-1234.png")
    private String meetingImagePath;

    @NotBlank
    @Size(max = 50)
    @Schema(example = "함께 읽는 에세이 모임")
    private String title;

    @NotBlank
    @Size(max = 300)
    @Schema(example = "매주 한 챕터씩 읽고 이야기해요.")
    private String description;

    @NotNull
    @Schema(example = "3")
    private Long readingGenreId;

    @NotNull
    @Min(3)
    @Max(8)
    @Schema(example = "8")
    private Byte capacity;

    @NotNull
    @Min(1)
    @Max(8)
    @Schema(example = "4")
    private Byte roundCount;

    @NotEmpty
    @Valid
    private List<RoundRequest> rounds;

    @NotNull
    @Valid
    private TimeRequest time;

    @NotEmpty
    @Valid
    private List<BookByRoundRequest> booksByRound;

    @Size(max = 300)
    @Schema(example = "안녕하세요, 함께 완독해봐요!")
    private String leaderIntro;

    @NotNull
    @Schema(example = "true")
    private Boolean leaderIntroSavePolicy;

    @Min(30)
    @Max(1440)
    @Schema(example = "90")
    private Integer durationMinutes;

    @NotNull
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    @Schema(example = "2026-01-12")
    private LocalDate firstRoundAt;

    @NotNull
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    @Schema(example = "2026-01-10")
    private LocalDate recruitmentDeadline;

    @AssertTrue(message = "startTime must be before endTime")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidTimeRange() {
        if (time == null || time.getStartTime() == null || time.getEndTime() == null) {
            return true;
        }
        return time.getStartTime().isBefore(time.getEndTime());
    }

    @AssertTrue(message = "roundCount must match rounds size")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidRoundCount() {
        if (roundCount == null || rounds == null) {
            return true;
        }
        return roundCount.equals(rounds.size());
    }

    @AssertTrue(message = "rounds must have unique roundNumber and match range")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidRoundNumbers() {
        if (roundCount == null || rounds == null) {
            return true;
        }
        Set<Byte> numbers = new HashSet<>();
        for (RoundRequest round : rounds) {
            if (round == null || round.getRoundNumber() == null) {
                return false;
            }
            if (round.getRoundNumber() < 1 || round.getRoundNumber() > roundCount) {
                return false;
            }
            if (!numbers.add(round.getRoundNumber())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "durationMinutes must be in 30-minute increments")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidDurationMinutes() {
        if (durationMinutes == null) {
            return true;
        }
        return durationMinutes % 30 == 0;
    }

    @Getter
    public static class RoundRequest {
        @NotNull
        @Min(1)
        @Schema(example = "1")
        private Byte roundNumber;

        @NotNull
        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        @Schema(example = "2026/01/12")
        private LocalDate date;
    }

    @Getter
    public static class TimeRequest {
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        @Schema(example = "20:00")
        private LocalTime startTime;

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        @Schema(example = "21:30")
        private LocalTime endTime;
    }

    @Getter
    public static class BookByRoundRequest {
        @NotNull
        @Min(1)
        @Schema(example = "1")
        private Byte roundNo;

        @NotNull
        @Valid
        private BookRequest book;
    }

    @Getter
    public static class BookRequest {
        @Size(max = 13)
        @Schema(example = "9781234567890")
        private String isbn13;

        @NotBlank
        @Size(max = 255)
        @Schema(example = "아몬드")
        private String title;

        @Schema(example = "[\"손원평\"]")
        private List<@NotBlank @Size(max = 100) String> authors;

        @Size(max = 255)
        @Schema(example = "출판사")
        private String publisher;

        @Size(max = 500)
        @Schema(example = "https://image.kr/book/1.jpg")
        private String thumbnailUrl;

        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        @Schema(example = "2020-01-01")
        private LocalDate publishedAt;
    }
}
