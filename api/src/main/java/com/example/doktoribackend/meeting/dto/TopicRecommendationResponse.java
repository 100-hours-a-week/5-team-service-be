package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "토론 주제 추천 응답")
public record TopicRecommendationResponse(
        @Schema(description = "모임 ID", example = "123")
        Long meetingId,

        @Schema(description = "회차 번호", example = "1")
        Integer roundNo,

        @Schema(description = "토론 주제 정보")
        TopicInfo topic,

        @Schema(description = "오늘 남은 AI 추천 횟수", example = "14")
        Integer remainingCount
) {
    @Builder
    @Schema(description = "토론 주제 정보")
    public record TopicInfo(
            @Schema(description = "주제 번호", example = "1")
            Integer topicNo,

            @Schema(description = "토론 주제", example = "주인공의 주요 선택들이 관계와 가치관을 어떻게 바꿨는지 토론합니다.")
            String topic,

            @Schema(description = "생성 주체 (AI/LEADER)", example = "AI")
            String source
    ) {
    }
}
