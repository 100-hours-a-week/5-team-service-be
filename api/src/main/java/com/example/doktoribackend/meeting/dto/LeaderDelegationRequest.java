package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "모임장 위임 요청")
public record LeaderDelegationRequest(
        @NotNull(message = "newLeaderMeetingMemberId는 필수입니다")
        @Positive(message = "newLeaderMeetingMemberId는 양의 정수여야 합니다")
        @Schema(description = "위임 대상 멤버의 meeting_members.id", example = "987")
        Long newLeaderMeetingMemberId
) {
}
