package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "참여 요청 상태 변경 요청")
public record ParticipationStatusUpdateRequest(
        @NotNull(message = "상태는 필수입니다")
        @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "상태는 APPROVED 또는 REJECTED만 가능합니다")
        @Schema(description = "변경할 상태", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED"})
        String status
) {
}
