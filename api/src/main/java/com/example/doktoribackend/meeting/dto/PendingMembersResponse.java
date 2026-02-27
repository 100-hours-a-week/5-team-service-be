package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "가입 신청 대기 멤버 목록 응답")
public record PendingMembersResponse(
        @Schema(description = "대기 멤버 목록")
        List<PendingMemberInfo> members,

        @Schema(description = "페이지 정보")
        PageInfo pageInfo
) {
    @Builder
    @Schema(description = "대기 멤버 정보")
    public record PendingMemberInfo(
            @Schema(description = "모임 멤버 ID", example = "15")
            Long meetingMemberId,

            @Schema(description = "닉네임", example = "readerC")
            String nickname,

            @Schema(description = "자기소개", example = "독서를 좋아하는 직장인입니다.")
            String memberIntro,

            @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profiles/15.png")
            String profileImagePath
    ) {
    }
}
