package com.example.doktoribackend.review.dto;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.review.domain.Review;
import com.example.doktoribackend.review.domain.ReviewImage;
import com.example.doktoribackend.user.domain.User;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record MyReviewDetailResponse(
        Long reviewId,
        String meetingTitle,
        Integer roundNo,
        String bookTitle,
        BigDecimal meetingRating,
        BigDecimal leaderRating,
        String content,
        Long bestMemberId,
        List<String> imageUrls,
        List<MemberInfo> members
) {
    public record MemberInfo(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {}

    public static MyReviewDetailResponse from(Review review,
                                               List<MeetingMember> approvedMembers,
                                               Long currentUserId,
                                               ImageUrlResolver imageUrlResolver) {
        List<String> imageUrls = review.getImages().stream()
                .sorted(Comparator.comparingInt(ReviewImage::getImageOrder))
                .map(img -> imageUrlResolver.toUrl(img.getImagePath()))
                .toList();

        List<MemberInfo> members = approvedMembers.stream()
                .filter(mm -> !mm.getUser().getId().equals(currentUserId))
                .map(mm -> {
                    User user = mm.getUser();
                    return new MemberInfo(
                            user.getId(),
                            user.getNickname(),
                            imageUrlResolver.toUrl(user.getProfileImagePath())
                    );
                })
                .toList();

        return new MyReviewDetailResponse(
                review.getId(),
                review.getMeetingTitle(),
                review.getRoundNo(),
                review.getBookTitle(),
                review.getMeetingRating(),
                review.getLeaderRating(),
                review.getContent(),
                review.getBestMemberId(),
                imageUrls,
                members
        );
    }
}
