package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.review.domain.Review;
import com.example.doktoribackend.review.domain.ReviewImage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record MyReviewResponse(
        Long reviewId,
        String meetingTitle,
        Integer roundNo,
        String bookTitle,
        BigDecimal meetingRating,
        String content,
        List<String> imageUrls
) {
    public static MyReviewResponse from(Review review,
                                        ImageUrlResolver imageUrlResolver) {
        List<String> imageUrls = review.getImages().stream()
                .sorted(Comparator.comparingInt(ReviewImage::getImageOrder))
                .map(img -> imageUrlResolver.toUrl(img.getImagePath()))
                .toList();

        return new MyReviewResponse(
                review.getId(),
                review.getMeetingTitle(),
                review.getRoundNo(),
                review.getBookTitle(),
                review.getMeetingRating(),
                review.getContent(),
                imageUrls
        );
    }
}
