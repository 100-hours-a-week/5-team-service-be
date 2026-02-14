package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.room.domain.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.example.doktoribackend.common.constants.ValidationConstant.*;

@Schema(description = "채팅 토론방 생성 요청")
public record ChatRoomCreateRequest(

        @Schema(description = "토론 주제", example = "AI가 인간의 일자리를 대체할 수 있는가")
        @NotBlank(message = "주제는 필수입니다.")
        @Size(min = TOPIC_MIN_LENGTH, max = TOPIC_MAX_LENGTH, message = "주제는 2~50자 사이여야 합니다.")
        @Pattern(regexp = TEXT_PATTERN, message = "주제에 특수문자나 이모지를 사용할 수 없습니다.")
        String topic,

        @Schema(description = "주제에 대한 한 줄 설명", example = "AI 기술 발전에 따른 고용 시장 변화를 토론합니다")
        @NotBlank(message = "설명은 필수입니다.")
        @Size(min = DESCRIPTION_MIN_LENGTH, max = DESCRIPTION_MAX_LENGTH, message = "설명은 2~50자 사이여야 합니다.")
        @Pattern(regexp = TEXT_PATTERN, message = "설명에 특수문자나 이모지를 사용할 수 없습니다.")
        String description,

        @Schema(description = "모집 정원 (2, 4, 6 중 선택)", example = "4")
        @NotNull(message = "정원은 필수입니다.")
        Integer capacity,

        @Schema(description = "방장의 토론 포지션", example = "AGREE")
        @NotNull(message = "포지션은 필수입니다.")
        Position position,

        @NotNull(message = "퀴즈는 필수입니다.")
        @Valid
        QuizRequest quiz
) {

    @Schema(description = "입장 퀴즈 정보")
    public record QuizRequest(

            @Schema(description = "퀴즈 질문", example = "대한민국의 수도는 어디인가요")
            @NotBlank(message = "퀴즈 질문은 필수입니다.")
            @Size(min = QUESTION_MIN_LENGTH, max = QUESTION_MAX_LENGTH, message = "퀴즈 질문은 2~50자 사이여야 합니다.")
            String question,

            @Schema(description = "퀴즈 선택지 목록 (4개 필수)")
            @NotNull(message = "선택지는 필수입니다.")
            @Size(min = CHOICE_COUNT, max = CHOICE_COUNT, message = "선택지는 4개여야 합니다.")
            @Valid
            List<QuizChoiceRequest> choices,

            @Schema(description = "정답 선택지 번호 (1~4)", example = "1")
            @NotNull(message = "정답 번호는 필수입니다.")
            @Min(value = CHOICE_NUMBER_MIN, message = "정답 번호는 1~4 사이여야 합니다.")
            @Max(value = CHOICE_NUMBER_MAX, message = "정답 번호는 1~4 사이여야 합니다.")
            Integer correctChoiceNumber
    ) {}

    @Schema(description = "퀴즈 선택지")
    public record QuizChoiceRequest(

            @Schema(description = "선택지 번호 (1~4)", example = "1")
            @NotNull(message = "선택지 번호는 필수입니다.")
            @Min(value = CHOICE_NUMBER_MIN, message = "선택지 번호는 1~4 사이여야 합니다.")
            @Max(value = CHOICE_NUMBER_MAX, message = "선택지 번호는 1~4 사이여야 합니다.")
            Integer choiceNumber,

            @Schema(description = "선택지 내용", example = "서울")
            @NotBlank(message = "선택지 내용은 필수입니다.")
            @Size(min = CHOICE_TEXT_MIN_LENGTH, max = CHOICE_TEXT_MAX_LENGTH, message = "선택지 내용은 2~100자 사이여야 합니다.")
            String text
    ) {}
}
