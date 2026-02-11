package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.common.validator.ValidImageUrl;
import com.example.doktoribackend.meeting.dto.validator.ValidMeetingCreateRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@ValidMeetingCreateRequest
public record MeetingCreateRequest(
        @NotBlank
        @ValidImageUrl
        @Schema(example = "images/meetings/36ba1999-7622-4275-b44e-9642d234b6bb.png")
        String meetingImagePath,

        @NotBlank
        @Size(max = 50)
        @Schema(example = "함께 읽는 에세이 모임")
        String title,

        @NotBlank
        @Size(max = 300)
        @Schema(example = "매주 한 챕터씩 읽고 이야기해요.")
        String description,

        @NotNull
        @Schema(example = "3")
        Long readingGenreId,

        @NotNull
        @Min(3)
        @Max(8)
        @Schema(example = "8")
        Integer capacity,

        @NotNull
        @Min(1)
        @Max(8)
        @Schema(example = "4")
        Integer roundCount,

        @NotEmpty
        @Valid
        List<RoundRequest> rounds,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        @Schema(example = "20:00")
        LocalTime startTime,

        @Size(max = 300)
        @Schema(example = "안녕하세요, 함께 완독해봐요!")
        String leaderIntro,

        @NotNull
        @Schema(example = "true")
        Boolean leaderIntroSavePolicy,

        @NotNull
        @Min(30)
        @Max(120)
        @Schema(example = "60")
        Integer durationMinutes,

        @NotNull
        @Schema(example = "2026-01-10")
        LocalDate recruitmentDeadline
) {
        public LocalDate firstRoundAt() {
                if (rounds == null || rounds.isEmpty()) {
                        return null;
                }
                return rounds.stream()
                        .filter(r -> r.roundNo() != null && r.roundNo() == 1)
                        .findFirst()
                        .map(RoundRequest::date)
                        .orElse(null);
        }
}
