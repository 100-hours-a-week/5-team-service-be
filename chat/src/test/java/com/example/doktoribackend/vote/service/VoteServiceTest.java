package com.example.doktoribackend.vote.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.vote.domain.Vote;
import com.example.doktoribackend.vote.domain.VoteCast;
import com.example.doktoribackend.vote.domain.VoteCastId;
import com.example.doktoribackend.vote.dto.VoteResultResponse;
import com.example.doktoribackend.vote.repository.VoteCastRepository;
import com.example.doktoribackend.vote.repository.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private VoteCastRepository voteCastRepository;

    @Mock
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @InjectMocks
    private VoteService voteService;

    private static final Long ROOM_ID = 10L;
    private static final Long USER_ID = 1L;

    private ChattingRoom createRoom() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제").description("설명").capacity(4).build();
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        return room;
    }

    private Vote createOpenVote() {
        Vote vote = Vote.create(createRoom(), 4);
        ReflectionTestUtils.setField(vote, "openedAt", LocalDateTime.now());
        return vote;
    }

    private Vote createExpiredVote() {
        Vote vote = Vote.create(createRoom(), 4);
        ReflectionTestUtils.setField(vote, "openedAt", LocalDateTime.now().minusMinutes(2));
        return vote;
    }

    private ChattingRoomMember createMember() {
        ChattingRoom room = createRoom();
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(room).userId(USER_ID).nickname("테스터")
                .profileImageUrl("http://profile.url")
                .role(MemberRole.PARTICIPANT).position(Position.AGREE).build();
        ReflectionTestUtils.setField(member, "status", MemberStatus.LEFT);
        return member;
    }

    @Nested
    @DisplayName("투표 생성")
    class CreateVote {

        @Test
        @DisplayName("채팅방 종료 시 투표가 생성된다")
        void createVote_success() {
            // given
            ChattingRoom room = createRoom();

            // when
            voteService.createVote(room, 4);

            // then
            then(voteRepository).should().save(any(Vote.class));
        }
    }

    @Nested
    @DisplayName("투표 참여")
    class CastVote {

        @Test
        @DisplayName("유효한 요청으로 투표에 참여한다")
        void castVote_success() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.existsById(new VoteCastId(ROOM_ID, USER_ID))).willReturn(false);

            // when
            voteService.castVote(ROOM_ID, USER_ID, Position.AGREE);

            // then
            then(voteCastRepository).should().save(any(VoteCast.class));
            assertThat(vote.getAgreeCount()).isEqualTo(1);
            assertThat(vote.getDisagreeCount()).isZero();
        }

        @Test
        @DisplayName("DISAGREE로 투표하면 disagreeCount가 증가한다")
        void castVote_disagree() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.existsById(new VoteCastId(ROOM_ID, USER_ID))).willReturn(false);

            // when
            voteService.castVote(ROOM_ID, USER_ID, Position.DISAGREE);

            // then
            assertThat(vote.getAgreeCount()).isZero();
            assertThat(vote.getDisagreeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("투표가 없으면 VOTE_NOT_FOUND 예외가 발생한다")
        void castVote_voteNotFound() {
            // given
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> voteService.castVote(ROOM_ID, USER_ID, Position.AGREE))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VOTE_NOT_FOUND);
        }

        @Test
        @DisplayName("투표 시간이 만료되면 VOTE_ALREADY_CLOSED 예외가 발생한다")
        void castVote_expired() {
            // given
            Vote vote = createExpiredVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));

            // when & then
            assertThatThrownBy(() -> voteService.castVote(ROOM_ID, USER_ID, Position.AGREE))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VOTE_ALREADY_CLOSED);

            assertThat(vote.isClosed()).isTrue();
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void castVote_notMember() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> voteService.castVote(ROOM_ID, USER_ID, Position.AGREE))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 투표했으면 VOTE_ALREADY_CAST 예외가 발생한다")
        void castVote_duplicate() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.existsById(new VoteCastId(ROOM_ID, USER_ID))).willReturn(true);

            // when & then
            assertThatThrownBy(() -> voteService.castVote(ROOM_ID, USER_ID, Position.AGREE))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VOTE_ALREADY_CAST);
        }
    }

    @Nested
    @DisplayName("투표 결과 조회")
    class GetVoteResult {

        @Test
        @DisplayName("투표 결과를 조회한다 (본인 투표 있음)")
        void getVoteResult_withMyChoice() {
            // given
            Vote vote = createOpenVote();
            ReflectionTestUtils.setField(vote, "agreeCount", 2);
            ReflectionTestUtils.setField(vote, "disagreeCount", 1);

            VoteCast myVoteCast = VoteCast.create(vote, USER_ID, Position.AGREE);

            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.findById(new VoteCastId(ROOM_ID, USER_ID)))
                    .willReturn(Optional.of(myVoteCast));

            // when
            VoteResultResponse response = voteService.getVoteResult(ROOM_ID, USER_ID);

            // then
            assertThat(response.agreeCount()).isEqualTo(2);
            assertThat(response.disagreeCount()).isEqualTo(1);
            assertThat(response.totalMemberCount()).isEqualTo(4);
            assertThat(response.isClosed()).isFalse();
            assertThat(response.myChoice()).isEqualTo(Position.AGREE);
        }

        @Test
        @DisplayName("투표 결과를 조회한다 (본인 투표 없음)")
        void getVoteResult_withoutMyChoice() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.findById(new VoteCastId(ROOM_ID, USER_ID)))
                    .willReturn(Optional.empty());

            // when
            VoteResultResponse response = voteService.getVoteResult(ROOM_ID, USER_ID);

            // then
            assertThat(response.myChoice()).isNull();
            assertThat(response.isClosed()).isFalse();
        }

        @Test
        @DisplayName("만료된 투표 조회 시 자동으로 종료 처리된다")
        void getVoteResult_autoClose() {
            // given
            Vote vote = createExpiredVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(createMember()));
            given(voteCastRepository.findById(new VoteCastId(ROOM_ID, USER_ID)))
                    .willReturn(Optional.empty());

            // when
            VoteResultResponse response = voteService.getVoteResult(ROOM_ID, USER_ID);

            // then
            assertThat(response.isClosed()).isTrue();
            assertThat(vote.isClosed()).isTrue();
        }

        @Test
        @DisplayName("투표가 없으면 VOTE_NOT_FOUND 예외가 발생한다")
        void getVoteResult_voteNotFound() {
            // given
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> voteService.getVoteResult(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VOTE_NOT_FOUND);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void getVoteResult_notMember() {
            // given
            Vote vote = createOpenVote();
            given(voteRepository.findById(ROOM_ID)).willReturn(Optional.of(vote));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> voteService.getVoteResult(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }
    }
}
