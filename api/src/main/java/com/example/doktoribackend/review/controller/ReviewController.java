package com.example.doktoribackend.review.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.review.dto.ReviewCreateRequest;
import com.example.doktoribackend.review.dto.ReviewCreateResponse;
import com.example.doktoribackend.review.service.ReviewService;
import com.example.doktoribackend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResult<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewCreateResponse response = reviewService.createReview(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(response));
    }
}
