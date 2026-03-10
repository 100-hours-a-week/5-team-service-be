package com.example.doktoribackend.review.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundStatus;
import com.example.doktoribackend.meeting.dto.PageInfo;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.review.domain.Review;
import com.example.doktoribackend.review.domain.ReviewImage;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.review.dto.*;
import com.example.doktoribackend.review.repository.ReviewRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MeetingRoundRepository meetingRoundRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final MeetingRepository meetingRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public ReviewCreateResponse createReview(Long userId, ReviewCreateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        MeetingRound meetingRound = meetingRoundRepository.findById(request.meetingRoundId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        if (meetingRound.getStatus() != MeetingRoundStatus.DONE) {
            throw new BusinessException(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endAt = meetingRound.getEndAt();
        if (!now.isAfter(endAt) || now.isAfter(endAt.plusHours(24))) {
            throw new BusinessException(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        Long meetingId = meetingRound.getMeeting().getId();

        boolean isMember = meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, MeetingMemberStatus.APPROVED);
        if (!isMember) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        boolean alreadySubmitted = reviewRepository.existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(
                request.meetingRoundId(), userId);
        if (alreadySubmitted) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_SUBMITTED);
        }

        if (request.bestMemberId() != null) {
            if (request.bestMemberId().equals(userId)) {
                throw new BusinessException(ErrorCode.INVALID_BEST_MEMBER);
            }
            boolean isBestMemberValid = meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                    meetingId, request.bestMemberId(), MeetingMemberStatus.APPROVED);
            if (!isBestMemberValid) {
                throw new BusinessException(ErrorCode.INVALID_BEST_MEMBER);
            }
        }

        Review review = Review.create(
                user,
                meetingRound,
                meetingRound.getMeeting().getTitle(),
                meetingRound.getRoundNo(),
                meetingRound.getBook().getTitle(),
                request.meetingRating(),
                request.leaderRating(),
                request.content(),
                request.bestMemberId()
        );

        List<String> imageKeys = request.imageKeys();
        if (imageKeys != null) {
            for (int i = 0; i < imageKeys.size(); i++) {
                ReviewImage image = ReviewImage.create(imageKeys.get(i), i + 1);
                review.addImage(image);
            }
        }

        reviewRepository.save(review);

        return new ReviewCreateResponse(review.getId());
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getReviewer().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_DELETE_FORBIDDEN);
        }

        review.softDelete();
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getLeaderReviews(Long meetingId, Long cursorId, int size) {
        Meeting meeting = meetingRepository.findByIdWithLeader(meetingId)
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND));

        Long leaderUserId = meeting.getLeaderUser().getId();

        List<Long> ids = reviewRepository.findIdsByLeaderUserIdWithCursor(
                leaderUserId, cursorId, PageRequest.of(0, size + 1));

        boolean hasNext = ids.size() > size;
        List<Long> contentIds = hasNext ? ids.subList(0, size) : ids;

        List<Review> reviews = contentIds.isEmpty()
                ? List.of()
                : reviewRepository.findAllWithReviewerAndImagesByIdIn(contentIds);

        List<ReviewResponse> items = reviews.stream()
                .map(review -> ReviewResponse.from(review, imageUrlResolver))
                .toList();

        Long nextCursorId = hasNext ? contentIds.getLast() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new ReviewListResponse(items, pageInfo);
    }

    @Transactional(readOnly = true)
    public MyReviewListResponse getMyReviews(Long userId, Long cursorId, int size) {
        List<Long> ids = reviewRepository.findIdsByReviewerIdWithCursor(
                userId, cursorId, PageRequest.of(0, size + 1));

        boolean hasNext = ids.size() > size;
        List<Long> contentIds = hasNext ? ids.subList(0, size) : ids;

        List<Review> reviews = contentIds.isEmpty()
                ? List.of()
                : reviewRepository.findAllWithImagesByIdIn(contentIds);

        List<MyReviewResponse> items = reviews.stream()
                .map(review -> MyReviewResponse.from(review, imageUrlResolver))
                .toList();

        Long nextCursorId = hasNext ? contentIds.getLast() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new MyReviewListResponse(items, pageInfo);
    }

    @Transactional(readOnly = true)
    public MyReviewDetailResponse getMyReviewDetail(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdWithReviewerAndRoundAndImages(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getReviewer().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }

        Long meetingId = review.getMeetingRound().getMeeting().getId();
        List<MeetingMember> approvedMembers =
                meetingMemberRepository.findApprovedMembersByMeetingIdOrderByCreatedAt(meetingId);

        return MyReviewDetailResponse.from(review, approvedMembers, userId, imageUrlResolver);
    }
}
