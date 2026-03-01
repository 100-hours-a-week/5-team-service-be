package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "토론 주제 추천 요청")
public record TopicRecommendationRequest(
        @NotNull(message = "topicNo는 필수입니다")
        @Min(value = 1, message = "topicNo는 1 이상이어야 합니다")
        @Max(value = 3, message = "topicNo는 3 이하여야 합니다")
        @Schema(description = "주제 번호 (1~3)", example = "1")
        Integer topicNo,

        @NotNull(message = "mode는 필수입니다")
        @Pattern(regexp = "^(AI|LEADER)$", message = "mode는 AI 또는 LEADER만 가능합니다")
        @Schema(description = "추천 모드 (AI: AI 추천, LEADER: 직접 입력)", example = "AI")
        String mode,

        @Size(max = 120, message = "주제는 120자 이하여야 합니다")
        @Schema(description = "토론 주제 (mode=LEADER일 때 필수)", example = "주인공의 선택에 대해 토론합니다")
        String topic
) {
    public boolean isAiMode() {
        return "AI".equals(mode);
    }

    public boolean isLeaderMode() {
        return "LEADER".equals(mode);
    }
}
