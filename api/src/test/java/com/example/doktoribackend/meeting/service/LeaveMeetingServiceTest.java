package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberRole;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import com.example.doktoribackend.meeting.repository.MeetingMemberRepository;
import com.example.doktoribackend.meeting.repository.MeetingRepository;
import com.example.doktoribackend.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("leaveMeeting: 모임 탈퇴")
class LeaveMeetingServiceTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingMemberRepository meetingMemberRepository;

    @Mock
    ImageUrlResolver imageUrlResolver;

    @InjectMocks
    MeetingService meetingService;

    private static final Long MEETING_ID = 10L;
    private static final Long LEADER_USER_ID = 1L;
    private static final Long MEMBER_USER_ID = 2L;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessTests {

        @Test
        @DisplayName("일반 멤버가 탈퇴하면 인원이 감소한다")
        void leaveMeeting_memberLeaves() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.RECRUITING, 5,
                    LocalDate.now().plusDays(7));
            MeetingMember member = createMember(MEMBER_USER_ID, MeetingMemberRole.MEMBER,
                    MeetingMemberStatus.APPROVED);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(member));

            // when
            meetingService.leaveMeeting(MEMBER_USER_ID, MEETING_ID);

            // then
            assertThat(meeting.getCurrentCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("정원이 찬 FINISHED 모임에서 멤버가 탈퇴하면 마감일 전이면 RECRUITING으로 복원된다")
        void leaveMeeting_restoreRecruiting() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.FINISHED, 8,
                    LocalDate.now().plusDays(7));
            ReflectionTestUtils.setField(meeting, "capacity", 8);
            MeetingMember member = createMember(MEMBER_USER_ID, MeetingMemberRole.MEMBER,
                    MeetingMemberStatus.APPROVED);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(member));

            // when
            meetingService.leaveMeeting(MEMBER_USER_ID, MEETING_ID);

            // then
            assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.RECRUITING);
            assertThat(meeting.getCurrentCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("정원이 찬 FINISHED 모임에서 멤버가 탈퇴해도 마감일이 지났으면 FINISHED를 유지한다")
        void leaveMeeting_pastDeadline_keepFinished() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.FINISHED, 8,
                    LocalDate.now().minusDays(1));
            ReflectionTestUtils.setField(meeting, "capacity", 8);
            MeetingMember member = createMember(MEMBER_USER_ID, MeetingMemberRole.MEMBER,
                    MeetingMemberStatus.APPROVED);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(member));

            // when
            meetingService.leaveMeeting(MEMBER_USER_ID, MEETING_ID);

            // then
            assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.FINISHED);
            assertThat(meeting.getCurrentCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("모임장 혼자 남았으면 탈퇴 가능하고 모임이 CANCELED 된다")
        void leaveMeeting_leaderAlone_canceled() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.RECRUITING, 1,
                    LocalDate.now().plusDays(7));
            MeetingMember leaderMember = createMember(LEADER_USER_ID, MeetingMemberRole.LEADER,
                    MeetingMemberStatus.APPROVED);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, LEADER_USER_ID))
                    .willReturn(Optional.of(leaderMember));

            // when
            meetingService.leaveMeeting(LEADER_USER_ID, MEETING_ID);

            // then
            assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CANCELED);
            assertThat(meeting.getCurrentCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureTests {

        @Test
        @DisplayName("모임장이 멤버가 남아있는 상태에서 탈퇴하면 LEADER_CANNOT_LEAVE 예외가 발생한다")
        void leaveMeeting_leaderWithMembers() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.RECRUITING, 3,
                    LocalDate.now().plusDays(7));
            MeetingMember leaderMember = createMember(LEADER_USER_ID, MeetingMemberRole.LEADER,
                    MeetingMemberStatus.APPROVED);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, LEADER_USER_ID))
                    .willReturn(Optional.of(leaderMember));

            // when & then
            assertThatThrownBy(() -> meetingService.leaveMeeting(LEADER_USER_ID, MEETING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        @Test
        @DisplayName("APPROVED 상태가 아닌 멤버가 탈퇴하면 LEAVE_NOT_ALLOWED 예외가 발생한다")
        void leaveMeeting_notApproved() {
            // given
            Meeting meeting = createMeeting(MeetingStatus.RECRUITING, 5,
                    LocalDate.now().plusDays(7));
            MeetingMember pendingMember = createMember(MEMBER_USER_ID, MeetingMemberRole.MEMBER,
                    MeetingMemberStatus.PENDING);

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));
            given(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(pendingMember));

            // when & then
            assertThatThrownBy(() -> meetingService.leaveMeeting(MEMBER_USER_ID, MEETING_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LEAVE_NOT_ALLOWED);
        }
    }

    // --- Helper Methods ---

    private Meeting createMeeting(MeetingStatus status, int currentCount, LocalDate recruitmentDeadline) {
        User leader = User.builder().nickname("leader").build();
        ReflectionTestUtils.setField(leader, "id", LEADER_USER_ID);

        Meeting meeting = Meeting.builder()
                .leaderUser(leader)
                .status(status)
                .capacity(8)
                .currentCount(currentCount)
                .recruitmentDeadline(recruitmentDeadline)
                .build();
        ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
        return meeting;
    }

    private MeetingMember createMember(Long userId, MeetingMemberRole role, MeetingMemberStatus status) {
        User user = User.builder().nickname("user" + userId).build();
        ReflectionTestUtils.setField(user, "id", userId);

        return MeetingMember.builder()
                .user(user)
                .role(role)
                .status(status)
                .build();
    }
}
