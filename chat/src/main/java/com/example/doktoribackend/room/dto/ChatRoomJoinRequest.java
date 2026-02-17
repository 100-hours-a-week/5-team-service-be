package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.room.domain.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.example.doktoribackend.common.constants.ValidationConstant.CHOICE_NUMBER_MAX;
import static com.example.doktoribackend.common.constants.ValidationConstant.CHOICE_NUMBER_MIN;

@Schema(description = "채팅방 참여 요청")
public record ChatRoomJoinRequest(

        @Schema(description = "토론 포지션", example = "AGREE")
        @NotNull(message = "포지션은 필수입니다.")
        Position position,

        @Schema(description = "퀴즈 정답 번호 (1~4)", example = "2")
        @NotNull(message = "퀴즈 답변은 필수입니다.")
        @Min(value = CHOICE_NUMBER_MIN, message = "퀴즈 답변은 1~4 사이여야 합니다.")
        @Max(value = CHOICE_NUMBER_MAX, message = "퀴즈 답변은 1~4 사이여야 합니다.")
        Integer quizAnswer
) {}
