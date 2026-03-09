package com.example.doktoribackend.review.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundStatus;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.review.domain.Review;
import com.example.doktoribackend.review.dto.ReviewCreateRequest;
import com.example.doktoribackend.review.dto.ReviewCreateResponse;
import com.example.doktoribackend.review.repository.ReviewRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import com.example.doktoribackend.book.domain.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingRoundRepository meetingRoundRepository;

    @Mock
    private MeetingMemberRepository meetingMemberRepository;

    @InjectMocks
    private ReviewService reviewService;

    private static final Long USER_ID = 1L;
    private static final Long MEETING_ID = 10L;
    private static final Long ROUND_ID = 100L;

    @Nested
    @DisplayName("createReview")
    class CreateReviewTests {

        @Test
        @DisplayName("정상적으로 리뷰를 생성한다")
        void success() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            mockValidContext();
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(review, "id", 1L);
                return review;
            });

            // when
            ReviewCreateResponse response = reviewService.createReview(USER_ID, request);

            // then
            assertThat(response.reviewId()).isEqualTo(1L);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("이미지와 함께 리뷰를 생성한다")
        void successWithImages() {
            // given
            ReviewCreateRequest request = createRequest(null, List.of("image1.jpg", "image2.jpg"));
            mockValidContext();
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(review, "id", 1L);
                return review;
            });

            // when
            ReviewCreateResponse response = reviewService.createReview(USER_ID, request);

            // then
            assertThat(response.reviewId()).isEqualTo(1L);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("bestMemberId를 포함하여 리뷰를 생성한다")
        void successWithBestMember() {
            // given
            Long bestMemberId = 2L;
            ReviewCreateRequest request = createRequest(bestMemberId, null);
            mockValidContext();
            given(meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                    MEETING_ID, bestMemberId, MeetingMemberStatus.APPROVED))
                    .willReturn(true);
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(review, "id", 1L);
                return review;
            });

            // when
            ReviewCreateResponse response = reviewService.createReview(USER_ID, request);

            // then
            assertThat(response.reviewId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 UserNotFoundException을 던진다")
        void userNotFound() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 회차면 ROUND_NOT_FOUND 에러를 던진다")
        void roundNotFound() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                    .willReturn(Optional.of(mock(User.class)));
            given(meetingRoundRepository.findById(ROUND_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_NOT_FOUND);
        }

        @Test
        @DisplayName("회차 상태가 DONE이 아니면 REVIEW_PERIOD_EXPIRED 에러를 던진다")
        void roundNotDone() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                    .willReturn(Optional.of(mock(User.class)));
            MeetingRound meetingRound = mock(MeetingRound.class);
            given(meetingRound.getStatus()).willReturn(MeetingRoundStatus.SCHEDULED);
            given(meetingRoundRepository.findById(ROUND_ID))
                    .willReturn(Optional.of(meetingRound));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        @Test
        @DisplayName("24시간 기간이 만료되면 REVIEW_PERIOD_EXPIRED 에러를 던진다")
        void reviewPeriodExpired() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                    .willReturn(Optional.of(mock(User.class)));
            MeetingRound meetingRound = mock(MeetingRound.class);
            given(meetingRound.getStatus()).willReturn(MeetingRoundStatus.DONE);
            given(meetingRound.getEndAt()).willReturn(LocalDateTime.now().minusHours(25));
            given(meetingRoundRepository.findById(ROUND_ID))
                    .willReturn(Optional.of(meetingRound));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        @Test
        @DisplayName("회차가 아직 끝나지 않았으면 REVIEW_PERIOD_EXPIRED 에러를 던진다")
        void roundNotEndedYet() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                    .willReturn(Optional.of(mock(User.class)));
            MeetingRound meetingRound = mock(MeetingRound.class);
            given(meetingRound.getStatus()).willReturn(MeetingRoundStatus.DONE);
            given(meetingRound.getEndAt()).willReturn(LocalDateTime.now().plusHours(1));
            given(meetingRoundRepository.findById(ROUND_ID))
                    .willReturn(Optional.of(meetingRound));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_PERIOD_EXPIRED);
        }

        @Test
        @DisplayName("모임 참여자가 아니면 AUTH_FORBIDDEN 에러를 던진다")
        void notMember() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            mockUserAndRound();
            given(meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                    MEETING_ID, USER_ID, MeetingMemberStatus.APPROVED))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
        }

        @Test
        @DisplayName("이미 리뷰를 작성했으면 REVIEW_ALREADY_SUBMITTED 에러를 던진다")
        void alreadySubmitted() {
            // given
            ReviewCreateRequest request = createRequest(null, null);
            mockUserAndRound();
            given(meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                    MEETING_ID, USER_ID, MeetingMemberStatus.APPROVED))
                    .willReturn(true);
            given(reviewRepository.existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(ROUND_ID, USER_ID))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_ALREADY_SUBMITTED);
        }

        @Test
        @DisplayName("bestMemberId가 본인이면 INVALID_BEST_MEMBER 에러를 던진다")
        void bestMemberIsSelf() {
            // given
            ReviewCreateRequest request = createRequest(USER_ID, null);
            mockValidContext();

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_BEST_MEMBER);
        }

        @Test
        @DisplayName("bestMemberId가 모임 멤버가 아니면 INVALID_BEST_MEMBER 에러를 던진다")
        void bestMemberNotInMeeting() {
            // given
            Long bestMemberId = 999L;
            ReviewCreateRequest request = createRequest(bestMemberId, null);
            mockValidContext();
            given(meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                    MEETING_ID, bestMemberId, MeetingMemberStatus.APPROVED))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_BEST_MEMBER);
        }
    }

    private ReviewCreateRequest createRequest(Long bestMemberId, List<String> imageKeys) {
        return new ReviewCreateRequest(
                ROUND_ID,
                new BigDecimal("4.5"),
                new BigDecimal("4.0"),
                "좋은 모임이었습니다",
                bestMemberId,
                imageKeys
        );
    }

    private void mockUserAndRound() {
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID))
                .willReturn(Optional.of(mock(User.class)));

        Meeting meeting = mock(Meeting.class);
        given(meeting.getId()).willReturn(MEETING_ID);
        given(meeting.getTitle()).willReturn("테스트 모임");

        Book book = mock(Book.class);
        given(book.getTitle()).willReturn("테스트 책");

        MeetingRound meetingRound = mock(MeetingRound.class);
        given(meetingRound.getStatus()).willReturn(MeetingRoundStatus.DONE);
        given(meetingRound.getEndAt()).willReturn(LocalDateTime.now().minusHours(1));
        given(meetingRound.getMeeting()).willReturn(meeting);
        given(meetingRound.getRoundNo()).willReturn(1);
        given(meetingRound.getBook()).willReturn(book);

        given(meetingRoundRepository.findById(ROUND_ID))
                .willReturn(Optional.of(meetingRound));
    }

    private void mockValidContext() {
        mockUserAndRound();
        given(meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                MEETING_ID, USER_ID, MeetingMemberStatus.APPROVED))
                .willReturn(true);
        given(reviewRepository.existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(ROUND_ID, USER_ID))
                .willReturn(false);
    }
}
