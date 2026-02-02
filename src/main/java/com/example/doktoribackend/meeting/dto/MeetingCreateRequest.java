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
    @Schema(example = "https://doktori-dev-images.s3.ap-northeast-2.amazonaws.com/images/meetings/36ba1999-7622-4275-b44e-9642d234b6bb.png")
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
    private Integer capacity;

    @NotNull
    @Min(1)
    @Max(8)
    @Schema(example = "4")
    private Integer roundCount;

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
    @Max(120)
    @Schema(example = "30")
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
        Set<Integer> numbers = new HashSet<>();
        for (RoundRequest round : rounds) {
            if (round == null || round.getRoundNo() == null) {
                return false;
            }
            if (round.getRoundNo() < 1 || round.getRoundNo() > roundCount) {
                return false;
            }
            if (!numbers.add(round.getRoundNo())) {
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

    @AssertTrue(message = "recruitmentDeadline must be today or future")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRecruitmentDeadlineNotPast() {
        if (recruitmentDeadline == null) {
            return true;
        }
        return !recruitmentDeadline.isBefore(LocalDate.now());
    }

    @AssertTrue(message = "firstRoundAt must be after today")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isFirstRoundAtAfterToday() {
        if (firstRoundAt == null) {
            return true;
        }
        return firstRoundAt.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "all round dates must be after today")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isAllRoundDatesAfterToday() {
        if (rounds == null || rounds.isEmpty()) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return rounds.stream()
                .allMatch(round -> round.getDate() != null && round.getDate().isAfter(today));
    }

    @AssertTrue(message = "round dates must be in chronological order")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRoundDatesInOrder() {
        if (rounds == null || rounds.size() < 2) {
            return true;
        }
        List<RoundRequest> sorted = rounds.stream()
                .filter(r -> r.getRoundNo() != null && r.getDate() != null)
                .sorted((a, b) -> a.getRoundNo().compareTo(b.getRoundNo()))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (!sorted.get(i).getDate().isAfter(sorted.get(i - 1).getDate())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "first round date must match firstRoundAt")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isFirstRoundDateMatchFirstRoundAt() {
        if (rounds == null || rounds.isEmpty() || firstRoundAt == null) {
            return true;
        }
        return rounds.stream()
                .filter(r -> r.getRoundNo() != null && r.getRoundNo() == 1)
                .findFirst()
                .map(r -> r.getDate() != null && r.getDate().equals(firstRoundAt))
                .orElse(false);
    }

    @AssertTrue(message = "booksByRound size must match roundCount")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isBooksByRoundCountMatch() {
        if (booksByRound == null || roundCount == null) {
            return true;
        }
        return booksByRound.size() == roundCount;
    }

    @AssertTrue(message = "booksByRound must have unique roundNo within valid range")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isValidBooksByRoundNumbers() {
        if (booksByRound == null || roundCount == null) {
            return true;
        }
        Set<Integer> roundNos = new HashSet<>();
        for (BookByRoundRequest req : booksByRound) {
            if (req.getRoundNo() == null || req.getRoundNo() < 1 || req.getRoundNo() > roundCount) {
                return false;
            }
            if (!roundNos.add(req.getRoundNo())) {
                return false;
            }
        }
        return true;
    }

    @Getter
    public static class RoundRequest {
        @NotNull
        @Min(1)
        @Schema(example = "1")
        private Integer roundNo;

        @NotNull
        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        @Schema(example = "2026-01-12")
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
        private Integer roundNo;

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

        @Size(max = 255)
        @Schema(example = "손원평")
        private String authors;

        @Size(max = 255)
        @Schema(example = "출판사")
        private String publisher;

        @Size(max = 500)
        @Schema(example = "https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=http%3A%2F%2Ft1.daumcdn.net%2Flbook%2Fimage%2F1467038")
        private String thumbnailUrl;

        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        @Schema(example = "2020-01-01")
        private LocalDate publishedAt;
    }
}
