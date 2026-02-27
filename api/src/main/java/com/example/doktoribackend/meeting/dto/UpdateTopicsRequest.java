package com.example.doktoribackend.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "토론 주제 추가/수정 요청")
public record UpdateTopicsRequest(
        @NotNull(message = "topics는 필수입니다")
        @Size(min = 3, max = 3, message = "topics는 정확히 3개여야 합니다")
        @Valid
        @Schema(description = "토론 주제 목록 (3개 필수)")
        List<TopicItem> topics
) {
    @Schema(description = "토론 주제 항목")
    public record TopicItem(
            @NotNull(message = "topicNo는 필수입니다")
            @Min(value = 1, message = "topicNo는 1 이상이어야 합니다")
            @Max(value = 3, message = "topicNo는 3 이하여야 합니다")
            @Schema(description = "주제 번호 (1~3)", example = "1")
            Integer topicNo,

            @NotNull(message = "topic은 필수입니다")
            @Size(min = 1, max = 120, message = "주제는 1~120자여야 합니다")
            @Schema(description = "토론 주제", example = "주인공의 선택에 대해 토론합니다")
            String topic,

            @NotNull(message = "source는 필수입니다")
            @Pattern(regexp = "^(AI|LEADER)$", message = "source는 AI 또는 LEADER만 가능합니다")
            @Schema(description = "생성 주체 (AI/LEADER)", example = "LEADER")
            String source
    ) {
    }
}
