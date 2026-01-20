package com.example.doktoribackend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "온보딩 요청")
public record OnboardingRequest(

        @Schema(description = "월 독서량 ID", example = "1")
        @Positive
        Long readingVolumeId,

        @Schema(description = "독서 목적 ID 목록", example = "[1, 2, 3]")
        @Size(max = 3, message = "독서 목적은 최대 3개 선택 가능합니다")
        List<@Positive Long> readingPurposeIds,

        @Schema(description = "선호 독서 장르 ID 목록", example = "[1, 2, 3]")
        @Size(max = 2, message = "선호 장르는 최대 2개 선택 가능합니다")
        List<@Positive Long> readingGenreIds
) {}