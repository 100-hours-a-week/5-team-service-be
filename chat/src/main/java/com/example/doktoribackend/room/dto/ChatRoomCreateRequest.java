package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.room.domain.Position;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRoomCreateRequest(

        @NotBlank(message = "주제는 필수입니다.")
        @Size(min = 2, max = 50, message = "주제는 2~50자 사이여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣\\s]+$", message = "주제에 특수문자나 이모지를 사용할 수 없습니다.")
        String topic,

        @NotBlank(message = "설명은 필수입니다.")
        @Size(min = 2, max = 50, message = "설명은 2~50자 사이여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣\\s]+$", message = "설명에 특수문자나 이모지를 사용할 수 없습니다.")
        String description,

        @NotNull(message = "정원은 필수입니다.")
        Integer capacity,

        @NotNull(message = "포지션은 필수입니다.")
        Position position,

        @NotNull(message = "퀴즈는 필수입니다.")
        @Valid
        QuizRequest quiz
) {

    public record QuizRequest(

            @NotBlank(message = "퀴즈 질문은 필수입니다.")
            @Size(min = 2, max = 50, message = "퀴즈 질문은 2~50자 사이여야 합니다.")
            String question,

            @NotNull(message = "선택지는 필수입니다.")
            @Size(min = 4, max = 4, message = "선택지는 4개여야 합니다.")
            @Valid
            List<QuizChoiceRequest> choices,

            @NotNull(message = "정답 번호는 필수입니다.")
            @Min(value = 1, message = "정답 번호는 1~4 사이여야 합니다.")
            @Max(value = 4, message = "정답 번호는 1~4 사이여야 합니다.")
            Integer correctChoiceNumber
    ) {}

    public record QuizChoiceRequest(

            @NotNull(message = "선택지 번호는 필수입니다.")
            @Min(value = 1, message = "선택지 번호는 1~4 사이여야 합니다.")
            @Max(value = 4, message = "선택지 번호는 1~4 사이여야 합니다.")
            Integer choiceNumber,

            @NotBlank(message = "선택지 내용은 필수입니다.")
            @Size(min = 2, max = 100, message = "선택지 내용은 2~100자 사이여야 합니다.")
            String text
    ) {}
}
