package com.example.doktoribackend.review.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.review.dto.ReviewCreateRequest;
import com.example.doktoribackend.review.dto.ReviewCreateResponse;
import com.example.doktoribackend.review.dto.ReviewListResponse;
import com.example.doktoribackend.review.service.ReviewService;
import com.example.doktoribackend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    @Override
    @PostMapping("/meeting-rounds/{meetingRoundId}")
    public ResponseEntity<ApiResult<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long meetingRoundId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewCreateResponse response = reviewService.createReview(userDetails.getId(), meetingRoundId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(response));
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(userDetails.getId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/meetings/{meetingId}")
    public ResponseEntity<ApiResult<ReviewListResponse>> getLeaderReviews(
            @PathVariable Long meetingId,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        if (size < 1 || size > 20) {
            throw new BusinessException(ErrorCode.PAGINATION_SIZE_OUT_OF_RANGE);
        }

        ReviewListResponse response = reviewService.getLeaderReviews(meetingId, cursorId, size);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
