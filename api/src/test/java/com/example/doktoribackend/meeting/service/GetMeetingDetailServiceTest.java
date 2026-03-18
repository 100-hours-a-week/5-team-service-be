package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.dto.MeetingDetailResponse;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.review.repository.ReviewRepository;
import com.example.doktoribackend.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("getMeetingDetail: 모임 상세 조회")
class GetMeetingDetailServiceTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingRoundRepository meetingRoundRepository;

    @Mock
    MeetingMemberRepository meetingMemberRepository;

    @Mock
    ReviewRepository reviewRepository;

    @Mock
    ImageUrlResolver imageUrlResolver;

    @InjectMocks
    MeetingService meetingService;

    private static final Long MEETING_ID = 10L;
    private static final Long LEADER_USER_ID = 1L;
    private static final Long CURRENT_USER_ID = 5L;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessTests {

        @Test
        @DisplayName("모임 상세를 조회하면 리더 평균 평점과 진행 횟수가 포함된다")
        void getMeetingDetail_withLeaderStats() {
            // given
            Meeting meeting = createMeeting();
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingRoundRepository.findByMeetingIdWithBook(MEETING_ID))
                    .willReturn(Collections.emptyList());
            given(reviewRepository.findAverageLeaderRatingByLeaderUserId(LEADER_USER_ID))
                    .willReturn(4.27);
            given(meetingMemberRepository.countActiveLeaderMeetingsByUserId(LEADER_USER_ID))
                    .willReturn(5L);
            given(imageUrlResolver.toUrl(anyString())).willReturn("https://cdn.example.com/image.png");

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(MEETING_ID, CURRENT_USER_ID);

            // then
            assertThat(response.getMeeting().getLeader().getAverageRating()).isEqualTo(4.3);
            assertThat(response.getMeeting().getLeader().getLeaderMeetingCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("리뷰가 없으면 averageRating이 null이다")
        void getMeetingDetail_noReviews() {
            // given
            Meeting meeting = createMeeting();
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingRoundRepository.findByMeetingIdWithBook(MEETING_ID))
                    .willReturn(Collections.emptyList());
            given(reviewRepository.findAverageLeaderRatingByLeaderUserId(LEADER_USER_ID))
                    .willReturn(null);
            given(meetingMemberRepository.countActiveLeaderMeetingsByUserId(LEADER_USER_ID))
                    .willReturn(1L);
            given(imageUrlResolver.toUrl(anyString())).willReturn("https://cdn.example.com/image.png");

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(MEETING_ID, CURRENT_USER_ID);

            // then
            assertThat(response.getMeeting().getLeader().getAverageRating()).isNull();
            assertThat(response.getMeeting().getLeader().getLeaderMeetingCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("평균 평점은 소수점 한 자리로 반올림된다")
        void getMeetingDetail_ratingRounded() {
            // given
            Meeting meeting = createMeeting();
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingRoundRepository.findByMeetingIdWithBook(MEETING_ID))
                    .willReturn(Collections.emptyList());
            given(reviewRepository.findAverageLeaderRatingByLeaderUserId(LEADER_USER_ID))
                    .willReturn(3.75);
            given(meetingMemberRepository.countActiveLeaderMeetingsByUserId(LEADER_USER_ID))
                    .willReturn(2L);
            given(imageUrlResolver.toUrl(anyString())).willReturn("https://cdn.example.com/image.png");

            // when
            MeetingDetailResponse response = meetingService.getMeetingDetail(MEETING_ID, CURRENT_USER_ID);

            // then
            assertThat(response.getMeeting().getLeader().getAverageRating()).isEqualTo(3.8);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureTests {

        @Test
        @DisplayName("모임이 존재하지 않으면 MEETING_NOT_FOUND 예외가 발생한다")
        void getMeetingDetail_meetingNotFound() {
            // given
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> meetingService.getMeetingDetail(MEETING_ID, CURRENT_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 모임이면 MEETING_NOT_FOUND 예외가 발생한다")
        void getMeetingDetail_deletedMeeting() {
            // given
            Meeting meeting = createMeeting();
            ReflectionTestUtils.setField(meeting, "deletedAt", LocalDateTime.now());
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            // when & then
            assertThatThrownBy(() -> meetingService.getMeetingDetail(MEETING_ID, CURRENT_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_NOT_FOUND);
        }
    }

    // --- Helper Methods ---

    private Meeting createMeeting() {
        User leader = User.builder()
                .nickname("독서왕")
                .profileImagePath("profiles/leader.png")
                .build();
        ReflectionTestUtils.setField(leader, "id", LEADER_USER_ID);

        Meeting meeting = Meeting.builder()
                .leaderUser(leader)
                .leaderIntro("안녕하세요")
                .title("함께 읽는 에세이 모임")
                .description("매주 한 챕터씩 읽고 이야기해요.")
                .status(MeetingStatus.RECRUITING)
                .capacity(8)
                .currentCount(5)
                .startTime(LocalTime.of(20, 0))
                .durationMinutes(90)
                .meetingImagePath("meetings/img.png")
                .build();
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        return meeting;
    }
}
