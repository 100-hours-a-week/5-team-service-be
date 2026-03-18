package com.example.doktoribackend.review.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingRoundStatus;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.review.domain.Review;
import com.example.doktoribackend.review.domain.ReviewImage;
import com.example.doktoribackend.review.dto.MyReviewDetailResponse;
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

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private com.example.doktoribackend.meeting.repository.MeetingRepository meetingRepository;

    @Mock
    private ImageUrlResolver imageUrlResolver;

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
            ReviewCreateResponse response = reviewService.createReview(USER_ID, ROUND_ID, request);

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
            ReviewCreateResponse response = reviewService.createReview(USER_ID, ROUND_ID, request);

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
            ReviewCreateResponse response = reviewService.createReview(USER_ID, ROUND_ID, request);

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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_NOT_FOUND);
        }

        @Test
        @DisplayName("회차 상태가 DONE이 아니면 ROUND_NOT_COMPLETED 에러를 던진다")
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_NOT_COMPLETED);
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
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
            BookReport approvedReport = mock(BookReport.class);
            given(approvedReport.getStatus()).willReturn(BookReportStatus.APPROVED);
            given(bookReportRepository.findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(USER_ID, ROUND_ID))
                    .willReturn(Optional.of(approvedReport));
            given(reviewRepository.existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(ROUND_ID, USER_ID))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
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
            assertThatThrownBy(() -> reviewService.createReview(USER_ID, ROUND_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_BEST_MEMBER);
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("정상적으로 리뷰를 삭제한다")
        void success() {
            // given
            Long reviewId = 1L;
            User reviewer = mock(User.class);
            given(reviewer.getId()).willReturn(USER_ID);

            Review review = mock(Review.class);
            given(review.getDeletedAt()).willReturn(null);
            given(review.getReviewer()).willReturn(reviewer);

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when
            reviewService.deleteReview(USER_ID, reviewId);

            // then
            verify(review).softDelete();
        }

        @Test
        @DisplayName("리뷰가 존재하지 않으면 REVIEW_NOT_FOUND 에러를 던진다")
        void reviewNotFound() {
            // given
            Long reviewId = 1L;
            given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, reviewId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 삭제된 리뷰면 REVIEW_NOT_FOUND 에러를 던진다")
        void alreadyDeleted() {
            // given
            Long reviewId = 1L;
            Review review = mock(Review.class);
            given(review.getDeletedAt()).willReturn(LocalDateTime.now());

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, reviewId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("작성자가 아니면 REVIEW_DELETE_FORBIDDEN 에러를 던진다")
        void notOwner() {
            // given
            Long reviewId = 1L;
            Long otherUserId = 999L;
            User reviewer = mock(User.class);
            given(reviewer.getId()).willReturn(otherUserId);

            Review review = mock(Review.class);
            given(review.getDeletedAt()).willReturn(null);
            given(review.getReviewer()).willReturn(reviewer);

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(USER_ID, reviewId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_DELETE_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("deleteAndRecreateReview")
    class DeleteAndRecreateReviewTests {

        @Test
        @DisplayName("삭제 후 동일 회차에 다시 리뷰를 작성할 수 있다")
        void canRecreateAfterDelete() {
            // given – 삭제
            Long reviewId = 1L;
            User reviewer = mock(User.class);
            given(reviewer.getId()).willReturn(USER_ID);

            Review review = mock(Review.class);
            given(review.getDeletedAt()).willReturn(null);
            given(review.getReviewer()).willReturn(reviewer);

            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            reviewService.deleteReview(USER_ID, reviewId);
            verify(review).softDelete();

            // given – 재작성: soft-deleted 리뷰는 중복 검사에 걸리지 않음
            ReviewCreateRequest request = createRequest(null, null);
            mockValidContext();
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> {
                Review saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 2L);
                return saved;
            });

            // when
            ReviewCreateResponse response = reviewService.createReview(USER_ID, ROUND_ID, request);

            // then
            assertThat(response.reviewId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("getMyReviewDetail")
    class GetMyReviewDetailTests {

        private static final Long REVIEW_ID = 1L;

        @Test
        @DisplayName("정상적으로 리뷰 상세를 조회한다")
        void success() {
            // given
            User reviewer = mock(User.class);
            given(reviewer.getId()).willReturn(USER_ID);

            MeetingRound meetingRound = mock(MeetingRound.class);
            given(meetingRound.getId()).willReturn(ROUND_ID);

            ReviewImage image = mock(ReviewImage.class);
            given(image.getImageOrder()).willReturn(1);
            given(image.getImagePath()).willReturn("reviews/img1.jpg");

            Review review = mock(Review.class);
            given(review.getId()).willReturn(REVIEW_ID);
            given(review.getReviewer()).willReturn(reviewer);
            given(review.getMeetingRound()).willReturn(meetingRound);
            given(review.getMeetingTitle()).willReturn("테스트 모임");
            given(review.getRoundNo()).willReturn(1);
            given(review.getBookTitle()).willReturn("테스트 책");
            given(review.getMeetingRating()).willReturn(new BigDecimal("4.5"));
            given(review.getLeaderRating()).willReturn(new BigDecimal("4.0"));
            given(review.getContent()).willReturn("좋은 모임이었습니다");
            given(review.getBestMemberId()).willReturn(2L);
            given(review.getImages()).willReturn(List.of(image));

            given(reviewRepository.findByIdWithReviewerAndRoundAndImages(REVIEW_ID))
                    .willReturn(Optional.of(review));

            User member2 = mock(User.class);
            given(member2.getId()).willReturn(2L);
            given(member2.getNickname()).willReturn("member2");
            given(member2.getProfileImagePath()).willReturn("profiles/2.jpg");

            User member3 = mock(User.class);
            given(member3.getId()).willReturn(3L);
            given(member3.getNickname()).willReturn("member3");
            given(member3.getProfileImagePath()).willReturn(null);

            BookReport br1 = mock(BookReport.class);
            given(br1.getUser()).willReturn(reviewer);

            BookReport br2 = mock(BookReport.class);
            given(br2.getUser()).willReturn(member2);

            BookReport br3 = mock(BookReport.class);
            given(br3.getUser()).willReturn(member3);

            given(bookReportRepository.findApprovedByMeetingRoundIdWithUser(ROUND_ID))
                    .willReturn(List.of(br1, br2, br3));

            given(imageUrlResolver.toUrl("reviews/img1.jpg")).willReturn("https://cdn.example.com/reviews/img1.jpg");
            given(imageUrlResolver.toUrl("profiles/2.jpg")).willReturn("https://cdn.example.com/profiles/2.jpg");
            given(imageUrlResolver.toUrl(null)).willReturn(null);

            // when
            MyReviewDetailResponse response = reviewService.getMyReviewDetail(USER_ID, REVIEW_ID);

            // then
            assertThat(response.reviewId()).isEqualTo(REVIEW_ID);
            assertThat(response.meetingTitle()).isEqualTo("테스트 모임");
            assertThat(response.leaderRating()).isEqualTo(new BigDecimal("4.0"));
            assertThat(response.bestMemberId()).isEqualTo(2L);
            assertThat(response.imageUrls()).containsExactly("https://cdn.example.com/reviews/img1.jpg");
            assertThat(response.members()).hasSize(2);
            assertThat(response.members().get(0).userId()).isEqualTo(2L);
            assertThat(response.members().get(0).nickname()).isEqualTo("member2");
        }

        @Test
        @DisplayName("리뷰가 존재하지 않으면 REVIEW_NOT_FOUND 에러를 던진다")
        void reviewNotFound() {
            // given
            given(reviewRepository.findByIdWithReviewerAndRoundAndImages(REVIEW_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviewDetail(USER_ID, REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("작성자가 아니면 REVIEW_NOT_FOUND 에러를 던진다")
        void notOwner() {
            // given
            Long otherUserId = 999L;
            User reviewer = mock(User.class);
            given(reviewer.getId()).willReturn(otherUserId);

            Review review = mock(Review.class);
            given(review.getReviewer()).willReturn(reviewer);

            given(reviewRepository.findByIdWithReviewerAndRoundAndImages(REVIEW_ID))
                    .willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.getMyReviewDetail(USER_ID, REVIEW_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        }
    }

    private ReviewCreateRequest createRequest(Long bestMemberId, List<String> imageKeys) {
        return new ReviewCreateRequest(
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
        BookReport approvedReport = mock(BookReport.class);
        given(approvedReport.getStatus()).willReturn(BookReportStatus.APPROVED);
        given(bookReportRepository.findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(USER_ID, ROUND_ID))
                .willReturn(Optional.of(approvedReport));
        given(reviewRepository.existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(ROUND_ID, USER_ID))
                .willReturn(false);
    }
}
