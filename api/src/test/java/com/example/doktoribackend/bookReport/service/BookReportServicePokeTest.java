package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRoundRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookReportServicePokeTest {

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private MeetingRoundRepository meetingRoundRepository;

    @Mock
    private MeetingMemberRepository meetingMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiValidationService aiValidationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookReportService bookReportService;

    private static final Long LEADER_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;
    private static final Long MEETING_ID = 100L;
    private static final Long ROUND_ID = 10L;
    private static final Long MEETING_MEMBER_ID = 20L;

    @Nested
    @DisplayName("pokeForBookReport")
    class PokeForBookReportTests {

        @Test
        @DisplayName("정상 케이스: 모임장이 미제출 멤버에게 찌르기 알림을 보낸다")
        void success() {
            // given
            Meeting meeting = mockMeeting();
            given(meeting.isLeader(LEADER_USER_ID)).willReturn(true);
            given(meeting.getTitle()).willReturn("테스트 모임");

            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().plusDays(2));
            MeetingMember targetMember = mockMember(TARGET_USER_ID, MeetingMemberStatus.APPROVED);

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));
            given(meetingMemberRepository.findByIdAndMeetingIdWithUser(MEETING_MEMBER_ID, MEETING_ID))
                    .willReturn(Optional.of(targetMember));
            given(bookReportRepository.findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(TARGET_USER_ID, ROUND_ID))
                    .willReturn(Optional.empty());

            // when
            bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID);

            // then
            verify(notificationService).createAndSend(
                    eq(TARGET_USER_ID),
                    eq(NotificationTypeCode.BOOK_REPORT_POKE),
                    eq(Map.of(
                            "meetingId", MEETING_ID.toString(),
                            "meetingTitle", "테스트 모임",
                            "roundNo", "1"
                    ))
            );
        }

        @Test
        @DisplayName("회차가 존재하지 않으면 ROUND_NOT_FOUND 예외를 던진다")
        void roundNotFound() {
            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_NOT_FOUND);
        }

        @Test
        @DisplayName("모임장이 아니면 BOOK_REPORT_MANAGEMENT_FORBIDDEN 예외를 던진다")
        void notLeader() {
            Long otherUserId = 999L;
            Meeting meeting = mockMeeting();
            // isLeader(999L) → default false (Mockito)

            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().plusDays(2));

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(otherUserId, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BOOK_REPORT_MANAGEMENT_FORBIDDEN);
        }

        @Test
        @DisplayName("독후감 제출 기간이 아니면 POKE_NOT_ALLOWED 예외를 던진다")
        void outsideSubmissionPeriod() {
            Meeting meeting = mockMeeting();
            given(meeting.isLeader(LEADER_USER_ID)).willReturn(true);

            // startAt이 이미 지난 경우 (deadline = startAt - 24h 도 지남)
            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().minusHours(1));

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.POKE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("대상 멤버가 존재하지 않으면 MEETING_MEMBER_NOT_FOUND 예외를 던진다")
        void memberNotFound() {
            Meeting meeting = mockMeeting();
            given(meeting.isLeader(LEADER_USER_ID)).willReturn(true);

            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().plusDays(2));

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));
            given(meetingMemberRepository.findByIdAndMeetingIdWithUser(MEETING_MEMBER_ID, MEETING_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("대상 멤버가 APPROVED 상태가 아니면 MEETING_MEMBER_NOT_FOUND 예외를 던진다")
        void memberNotApproved() {
            Meeting meeting = mockMeeting();
            given(meeting.isLeader(LEADER_USER_ID)).willReturn(true);

            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().plusDays(2));
            MeetingMember targetMember = mockMember(TARGET_USER_ID, MeetingMemberStatus.LEFT);

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));
            given(meetingMemberRepository.findByIdAndMeetingIdWithUser(MEETING_MEMBER_ID, MEETING_ID))
                    .willReturn(Optional.of(targetMember));

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 독후감을 제출한 멤버에게는 POKE_TARGET_ALREADY_SUBMITTED 예외를 던진다")
        void alreadySubmitted() {
            Meeting meeting = mockMeeting();
            given(meeting.isLeader(LEADER_USER_ID)).willReturn(true);

            MeetingRound round = mockRound(meeting, 1, LocalDateTime.now().plusDays(2));
            MeetingMember targetMember = mockMember(TARGET_USER_ID, MeetingMemberStatus.APPROVED);

            given(meetingRoundRepository.findByIdWithBookAndMeeting(ROUND_ID))
                    .willReturn(Optional.of(round));
            given(meetingMemberRepository.findByIdAndMeetingIdWithUser(MEETING_MEMBER_ID, MEETING_ID))
                    .willReturn(Optional.of(targetMember));
            given(bookReportRepository.findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(TARGET_USER_ID, ROUND_ID))
                    .willReturn(Optional.of(mock(BookReport.class)));

            assertThatThrownBy(() -> bookReportService.pokeForBookReport(LEADER_USER_ID, ROUND_ID, MEETING_MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.POKE_TARGET_ALREADY_SUBMITTED);

            verify(notificationService, never()).createAndSend(any(), any(), anyMap());
        }
    }

    private Meeting mockMeeting() {
        Meeting meeting = mock(Meeting.class);
        lenient().when(meeting.getId()).thenReturn(MEETING_ID);
        return meeting;
    }

    private MeetingRound mockRound(Meeting meeting, int roundNo, LocalDateTime startAt) {
        MeetingRound round = mock(MeetingRound.class);
        given(round.getMeeting()).willReturn(meeting);
        lenient().when(round.getRoundNo()).thenReturn(roundNo);
        lenient().when(round.getStartAt()).thenReturn(startAt);
        return round;
    }

    private MeetingMember mockMember(Long userId, MeetingMemberStatus status) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);

        MeetingMember member = mock(MeetingMember.class);
        lenient().when(member.getUser()).thenReturn(user);
        given(member.getStatus()).willReturn(status);
        return member;
    }
}
