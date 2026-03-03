package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.common.validator.ValidImageUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "모임 부분 수정 요청")
public record MeetingPatchRequest(
        @NotBlank
        @ValidImageUrl
        @Schema(description = "모임 이미지 키", example = "images/meetings/36ba1999-7622-4275-b44e-9642d234b6bb.png")
        String meetingImageKey,

        @NotBlank
        @Size(max = 50)
        @Schema(description = "모임 제목", example = "함께 읽는 에세이 모임")
        String title,

        @NotBlank
        @Size(max = 300)
        @Schema(description = "모임 설명", example = "매주 한 챕터씩 읽고 이야기해요.")
        String description,

        @NotNull
        @Schema(description = "독서 장르 ID", example = "3")
        Long readingGenreId,

        @NotNull
        @Min(3)
        @Max(8)
        @Schema(description = "정원", example = "8")
        Integer capacity,

        @NotNull
        @Schema(description = "모집 마감일", example = "2026-01-10")
        LocalDate recruitmentDeadline,

        @Size(max = 300)
        @Schema(description = "모임장 소개", example = "안녕하세요, 함께 완독해봐요!")
        String leaderIntro,

        @NotNull
        @Schema(description = "모임장 소개 저장 여부", example = "true")
        Boolean leaderIntroSavePolicy
) {
}
