package com.example.doktoribackend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Schema(description = "참여 요청 목록 응답")
public record ParticipationListResponse(
        @Schema(description = "모임 ID", example = "123")
        Long meetingId,

        @Schema(description = "참여 요청 목록")
        List<ParticipationItem> items,

        @Schema(description = "페이지 정보")
        PageInfo pageInfo
) {
    @Builder
    @Schema(description = "참여 요청 항목")
    public record ParticipationItem(
            @Schema(description = "모임 멤버 ID", example = "11")
            Long meetingMemberId,

            @Schema(description = "상태", example = "PENDING")
            String status,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            @Schema(description = "요청 일시", example = "2026-01-10T21:00:00+09:00")
            OffsetDateTime requestedAt,

            @Schema(description = "닉네임", example = "readerA")
            String nickname,

            @Schema(description = "멤버 소개", example = "열심히 참여하는 모임원이 되겠습니다!")
            String memberIntro,

            @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profiles/11.png")
            String profileImagePath
    ) {
    }
}
