package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.review.dto.validator.ValidRatingStep;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ReviewCreateRequest(
        @NotNull(message = "모임 회차 ID는 필수입니다")
        Long meetingRoundId,

        @NotNull(message = "모임 별점은 필수입니다")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
        @ValidRatingStep
        BigDecimal meetingRating,

        @NotNull(message = "모임장 별점은 필수입니다")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
        @ValidRatingStep
        BigDecimal leaderRating,

        @Size(max = 200, message = "리뷰 내용은 200자 이내여야 합니다")
        String content,

        Long bestMemberId,

        @Size(max = 5, message = "리뷰 이미지는 최대 5장까지 가능합니다")
        List<String> imageKeys
) {
}
