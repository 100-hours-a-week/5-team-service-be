package com.example.doktoribackend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
@Schema(description = "모임 멤버 목록 응답")
public record MeetingMembersResponse(
        @Schema(description = "모임 ID", example = "123")
        Long meetingId,

        @Schema(description = "멤버 수", example = "2")
        Integer memberCount,

        @Schema(description = "멤버 목록")
        List<MemberInfo> members
) {
    @Builder
    @Schema(description = "멤버 정보")
    public record MemberInfo(
            @Schema(description = "모임 멤버 ID", example = "11")
            Long meetingMemberId,

            @Schema(description = "닉네임", example = "readerA")
            String nickname,

            @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profiles/11.png")
            String profileImagePath,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            @Schema(description = "가입 일시", example = "2026-01-10T20:00:00+09:00")
            OffsetDateTime joinedAt
    ) {
    }
}
