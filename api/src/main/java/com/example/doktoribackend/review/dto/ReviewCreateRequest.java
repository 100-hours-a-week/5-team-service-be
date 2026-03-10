package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.review.dto.validator.ValidRatingStep;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "리뷰 생성 요청")
public record ReviewCreateRequest(
        @Schema(description = "모임 회차 ID", example = "1")
        @NotNull(message = "모임 회차 ID는 필수입니다")
        Long meetingRoundId,

        @Schema(description = "모임 별점 (0.5~5.0, 0.5 단위)", example = "4.5")
        @NotNull(message = "모임 별점은 필수입니다")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
        @ValidRatingStep
        BigDecimal meetingRating,

        @Schema(description = "모임장 별점 (0.5~5.0, 0.5 단위)", example = "4.0")
        @NotNull(message = "모임장 별점은 필수입니다")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다")
        @ValidRatingStep
        BigDecimal leaderRating,

        @Schema(description = "리뷰 내용 (최대 200자)", example = "좋은 모임이었습니다. 다음에도 참여하고 싶어요!")
        @Size(max = 200, message = "리뷰 내용은 200자 이내여야 합니다")
        String content,

        @Schema(description = "베스트 모임원 사용자 ID", example = "2")
        Long bestMemberId,

        @Schema(description = "리뷰 이미지 키 목록 (최대 5장)", example = "[\"images/meetings/f627f127-dd52-4175-a89c-a0d3cc08d03a.jpg\", \"images/meetings/cd273683-7039-43c1-b043-3c53af572a9f.jpeg\", \"images/meetings/30e68c00-f3dc-474e-8941-db2af472519f.jpeg\", \"images/meetings/07ca04d8-6e09-4f8f-8bea-c6522e421368.jpeg\", \"images/meetings/e7b4ef10-5ac9-4e06-91f7-ac7f0f941adf.jpeg\"]")
        @Size(max = 5, message = "리뷰 이미지는 최대 5장까지 가능합니다")
        List<String> imageKeys
) {
}
