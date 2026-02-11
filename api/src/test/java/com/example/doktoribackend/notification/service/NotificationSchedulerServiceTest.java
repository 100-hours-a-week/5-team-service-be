package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerServiceTest {

    @Mock
    private MeetingRoundRepository meetingRoundRepository;

    @Mock
    private MeetingMemberRepository meetingMemberRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationSchedulerService schedulerService;

    @Nested
    @DisplayName("독후감 마감 24시간 전 알림")
    class ReviewDeadline24hNotificationTests {

        @Test
        @DisplayName("대상 회차가 있으면 멤버들에게 알림을 발송한다")
        void sendReviewDeadline24hNotifications_success() {
            // given
            MeetingRound round = createMockMeetingRound(1L, 100L);
            List<Long> memberIds = List.of(1L, 2L);

            given(meetingRoundRepository.findRoundsStartingBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(List.of(round));
            given(meetingMemberRepository.findApprovedMemberUserIds(100L))
                    .willReturn(memberIds);

            // when
            schedulerService.sendReviewDeadline24hNotifications();

            // then
            verify(notificationService).createAndSendBatch(
                    memberIds,
                    NotificationTypeCode.BOOK_REPORT_DEADLINE_24H_BEFORE,
                    Map.of("meetingId", "100", "meetingTitle", "테스트 모임")
            );
        }

        @Test
        @DisplayName("대상 회차가 없으면 아무것도 하지 않는다")
        void sendReviewDeadline24hNotifications_noRounds_doesNothing() {
            // given
            given(meetingRoundRepository.findRoundsStartingBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            schedulerService.sendReviewDeadline24hNotifications();

            // then
            verify(notificationService, never()).createAndSendBatch(anyList(), any(), any());
        }
    }

    @Nested
    @DisplayName("독후감 마감 30분 전 알림")
    class ReviewDeadline30mNotificationTests {

        @Test
        @DisplayName("대상 회차가 있으면 멤버들에게 알림을 발송한다")
        void sendReviewDeadline30mNotifications_success() {
            // given
            MeetingRound round = createMockMeetingRound(1L, 100L);
            List<Long> memberIds = List.of(1L, 2L);

            given(meetingRoundRepository.findRoundsStartingBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(List.of(round));
            given(meetingMemberRepository.findApprovedMemberUserIds(100L))
                    .willReturn(memberIds);

            // when
            schedulerService.sendReviewDeadline30mNotifications();

            // then
            verify(notificationService).createAndSendBatch(
                    memberIds,
                    NotificationTypeCode.BOOK_REPORT_DEADLINE_30M_BEFORE,
                    Map.of("meetingId", "100", "meetingTitle", "테스트 모임")
            );
        }

        @Test
        @DisplayName("대상 회차가 없으면 아무것도 하지 않는다")
        void sendReviewDeadline30mNotifications_noRounds_doesNothing() {
            // given
            given(meetingRoundRepository.findRoundsStartingBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            schedulerService.sendReviewDeadline30mNotifications();

            // then
            verify(notificationService, never()).createAndSendBatch(anyList(), any(), any());
        }
    }

    private MeetingRound createMockMeetingRound(Long roundId, Long meetingId) {
        Meeting meeting = mock(Meeting.class);
        given(meeting.getId()).willReturn(meetingId);
        given(meeting.getTitle()).willReturn("테스트 모임");

        MeetingRound round = mock(MeetingRound.class);
        given(round.getId()).willReturn(roundId);
        given(round.getMeeting()).willReturn(meeting);

        return round;
    }
}
