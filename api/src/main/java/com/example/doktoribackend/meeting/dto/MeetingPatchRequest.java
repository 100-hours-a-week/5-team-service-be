package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.common.validator.ValidImageUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "모임 부분 수정 요청")
public record MeetingPatchRequest(
        @ValidImageUrl
        @Schema(description = "모임 이미지 키", example = "images/meetings/36ba1999-7622-4275-b44e-9642d234b6bb.png")
        String meetingImageKey,

        @Size(max = 50)
        @Schema(description = "모임 제목", example = "함께 읽는 에세이 모임")
        String title,

        @Size(max = 300)
        @Schema(description = "모임 설명", example = "매주 한 챕터씩 읽고 이야기해요.")
        String description,

        @Schema(description = "독서 장르 ID", example = "3")
        Long readingGenreId,

        @Min(3)
        @Max(8)
        @Schema(description = "정원", example = "8")
        Integer capacity,

        @Schema(description = "모집 마감일", example = "2026-01-10")
        LocalDate recruitmentDeadline,

        @Size(max = 300)
        @Schema(description = "모임장 소개", example = "안녕하세요, 함께 완독해봐요!")
        String leaderIntro,

        @Schema(description = "모임장 소개 저장 여부", example = "true")
        Boolean leaderIntroSavePolicy
) {
    /**
     * 최소 하나의 필드가 전달되었는지 확인
     */
    public boolean hasAnyField() {
        return meetingImageKey != null ||
                title != null ||
                description != null ||
                readingGenreId != null ||
                capacity != null ||
                recruitmentDeadline != null ||
                leaderIntro != null ||
                leaderIntroSavePolicy != null;
    }
}
