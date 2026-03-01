package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "토론 주제 추가/수정 응답")
public record UpdateTopicsResponse(
        @Schema(description = "토론 주제 목록")
        List<TopicItem> topics
) {
    @Builder
    @Schema(description = "토론 주제 항목")
    public record TopicItem(
            @Schema(description = "주제 번호", example = "1")
            Integer topicNo,

            @Schema(description = "토론 주제", example = "주인공의 선택에 대해 토론합니다")
            String topic,

            @Schema(description = "생성 주체 (AI/LEADER)", example = "LEADER")
            String source
    ) {
    }
}
