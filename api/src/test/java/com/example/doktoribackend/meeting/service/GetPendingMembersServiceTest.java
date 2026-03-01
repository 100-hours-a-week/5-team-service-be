package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberRole;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import com.example.doktoribackend.meeting.dto.PendingMembersResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("getPendingMembers: 가입 신청 대기 멤버 조회")
class GetPendingMembersServiceTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingMemberRepository meetingMemberRepository;

    @Mock
    ImageUrlResolver imageUrlResolver;

    @InjectMocks
    MeetingService meetingService;

    private static final Long LEADER_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long MEETING_ID = 10L;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessTests {

        @Test
        @DisplayName("PENDING 상태의 멤버 목록을 최신순으로 반환한다")
        void getPendingMembers_success() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            MeetingMember member1 = createPendingMember(15L, "readerC", "독서를 좋아합니다.", "profiles/c.png");
            MeetingMember member2 = createPendingMember(14L, "readerB", "매일 읽고 있습니다.", "profiles/b.png");

            given(meetingMemberRepository.findPendingMembersByMeetingIdWithCursor(
                    MEETING_ID, null, PageRequest.of(0, 21)))
                    .willReturn(List.of(member1, member2));

            given(imageUrlResolver.toUrl("profiles/c.png")).willReturn("https://cdn.example.com/profiles/c.png");
            given(imageUrlResolver.toUrl("profiles/b.png")).willReturn("https://cdn.example.com/profiles/b.png");

            // when
            PendingMembersResponse response = meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, null, 20);

            // then
            assertThat(response.members()).hasSize(2);
            assertThat(response.pageInfo().isHasNext()).isFalse();
            assertThat(response.pageInfo().getNextCursorId()).isNull();
            assertThat(response.pageInfo().getSize()).isEqualTo(20);

            PendingMembersResponse.PendingMemberInfo first = response.members().get(0);
            assertThat(first.meetingMemberId()).isEqualTo(15L);
            assertThat(first.nickname()).isEqualTo("readerC");
            assertThat(first.memberIntro()).isEqualTo("독서를 좋아합니다.");
            assertThat(first.profileImagePath()).isEqualTo("https://cdn.example.com/profiles/c.png");
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext=true, nextCursorId를 반환한다")
        void getPendingMembers_hasNext() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            int size = 2;
            // size + 1 = 3개 반환하면 hasNext = true
            MeetingMember member1 = createPendingMember(20L, "a", "intro1", "p/a.png");
            MeetingMember member2 = createPendingMember(19L, "b", "intro2", "p/b.png");
            MeetingMember member3 = createPendingMember(18L, "c", "intro3", "p/c.png");

            given(meetingMemberRepository.findPendingMembersByMeetingIdWithCursor(
                    MEETING_ID, null, PageRequest.of(0, size + 1)))
                    .willReturn(List.of(member1, member2, member3));

            given(imageUrlResolver.toUrl("p/a.png")).willReturn("https://cdn/p/a.png");
            given(imageUrlResolver.toUrl("p/b.png")).willReturn("https://cdn/p/b.png");

            // when
            PendingMembersResponse response = meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, null, size);

            // then
            assertThat(response.members()).hasSize(2);
            assertThat(response.pageInfo().isHasNext()).isTrue();
            assertThat(response.pageInfo().getNextCursorId()).isEqualTo(19L);
        }

        @Test
        @DisplayName("cursorId를 전달하면 해당 커서 이후 데이터를 조회한다")
        void getPendingMembers_withCursorId() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            Long cursorId = 15L;
            MeetingMember member = createPendingMember(14L, "reader", "intro", "p/r.png");

            given(meetingMemberRepository.findPendingMembersByMeetingIdWithCursor(
                    MEETING_ID, cursorId, PageRequest.of(0, 21)))
                    .willReturn(List.of(member));

            given(imageUrlResolver.toUrl("p/r.png")).willReturn("https://cdn/p/r.png");

            // when
            PendingMembersResponse response = meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, cursorId, 20);

            // then
            assertThat(response.members()).hasSize(1);
            assertThat(response.members().get(0).meetingMemberId()).isEqualTo(14L);
            assertThat(response.pageInfo().isHasNext()).isFalse();

            then(meetingMemberRepository).should()
                    .findPendingMembersByMeetingIdWithCursor(MEETING_ID, cursorId, PageRequest.of(0, 21));
        }

        @Test
        @DisplayName("PENDING 멤버가 없으면 빈 목록을 반환한다")
        void getPendingMembers_empty() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            given(meetingMemberRepository.findPendingMembersByMeetingIdWithCursor(
                    MEETING_ID, null, PageRequest.of(0, 21)))
                    .willReturn(Collections.emptyList());

            // when
            PendingMembersResponse response = meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, null, 20);

            // then
            assertThat(response.members()).isEmpty();
            assertThat(response.pageInfo().isHasNext()).isFalse();
            assertThat(response.pageInfo().getNextCursorId()).isNull();
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureTests {

        @Test
        @DisplayName("모임이 존재하지 않으면 MEETING_NOT_FOUND 예외가 발생한다")
        void getPendingMembers_meetingNotFound() {
            // given
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, null, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 모임이면 MEETING_NOT_FOUND 예외가 발생한다")
        void getPendingMembers_deletedMeeting() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            ReflectionTestUtils.setField(meeting, "deletedAt", java.time.LocalDateTime.now());

            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            // when & then
            assertThatThrownBy(() -> meetingService.getPendingMembers(
                    LEADER_USER_ID, MEETING_ID, null, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_NOT_FOUND);
        }

        @Test
        @DisplayName("모임장이 아니면 AUTH_FORBIDDEN 예외가 발생한다")
        void getPendingMembers_notLeader() {
            // given
            Meeting meeting = createMeetingWithLeader(MEETING_ID, LEADER_USER_ID);
            given(meetingRepository.findByIdWithLeader(MEETING_ID))
                    .willReturn(Optional.of(meeting));

            // when & then
            assertThatThrownBy(() -> meetingService.getPendingMembers(
                    OTHER_USER_ID, MEETING_ID, null, 20))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
        }
    }

    // --- Helper Methods ---

    private Meeting createMeetingWithLeader(Long meetingId, Long leaderUserId) {
        User leader = User.builder().nickname("leader").build();
        ReflectionTestUtils.setField(leader, "id", leaderUserId);

        Meeting meeting = Meeting.builder()
                .leaderUser(leader)
                .build();
        ReflectionTestUtils.setField(meeting, "id", meetingId);
        return meeting;
    }

    private MeetingMember createPendingMember(Long memberId, String nickname, String memberIntro, String profileImagePath) {
        User user = User.builder()
                .nickname(nickname)
                .profileImagePath(profileImagePath)
                .memberIntro(memberIntro)
                .build();

        MeetingMember member = MeetingMember.builder()
                .user(user)
                .role(MeetingMemberRole.MEMBER)
                .status(MeetingMemberStatus.PENDING)
                .memberIntro(memberIntro)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
