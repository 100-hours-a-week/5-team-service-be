package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "참여 요청 상태 변경 응답")
public record ParticipationStatusUpdateResponse(
        @Schema(description = "모임 ID", example = "123")
        Long meetingId,

        @Schema(description = "참여 요청 ID", example = "987")
        Long joinRequestId,

        @Schema(description = "변경된 상태", example = "APPROVED")
        String status
) {
}
