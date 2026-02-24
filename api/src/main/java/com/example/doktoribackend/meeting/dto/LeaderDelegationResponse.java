package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "모임장 위임 응답")
public record LeaderDelegationResponse(
        @Schema(description = "모임 ID", example = "123")
        Long meetingId,

        @Schema(description = "새 리더의 meeting_members.id", example = "987")
        Long leaderMeetingMemberId
) {
}
