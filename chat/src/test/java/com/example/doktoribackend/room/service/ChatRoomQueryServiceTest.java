package com.example.doktoribackend.room.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChatRoomQueryServiceTest {

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Mock
    private RoomRoundRepository roomRoundRepository;

    @Mock
    private ImageUrlResolver imageUrlResolver;

    @InjectMocks
    private ChatRoomQueryService chatRoomQueryService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;

    @Nested
    @DisplayName("채팅방 목록 조회")
    class GetChatRooms {

        private ChattingRoom createRoom(Long id, String topic, int capacity, int currentMemberCount) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic(topic)
                    .description("설명")
                    .capacity(capacity)
                    .build();
            ReflectionTestUtils.setField(room, "id", id);
            ReflectionTestUtils.setField(room, "currentMemberCount", currentMemberCount);
            return room;
        }

        @Test
        @DisplayName("WAITING 상태의 방 목록을 반환한다")
        void getChatRooms_returnsWaitingRooms() {
            // given
            int size = 10;
            List<ChattingRoom> rooms = List.of(
                    createRoom(3L, "주제3", 4, 2),
                    createRoom(2L, "주제2", 6, 1)
            );
            given(chattingRoomRepository.findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(null), any(PageRequest.class)))
                    .willReturn(rooms);

            // when
            ChatRoomListResponse response = chatRoomQueryService.getChatRooms(null, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items().getFirst().roomId()).isEqualTo(3L);
            assertThat(response.items().getFirst().topic()).isEqualTo("주제3");
            assertThat(response.items().get(0).capacity()).isEqualTo(4);
            assertThat(response.items().get(0).currentMemberCount()).isEqualTo(2);
            assertThat(response.items().get(1).roomId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext=true, nextCursorId가 마지막 항목의 id이다")
        void getChatRooms_hasNext_true() {
            // given
            int size = 2;
            List<ChattingRoom> rooms = List.of(
                    createRoom(5L, "주제5", 4, 1),
                    createRoom(4L, "주제4", 4, 2),
                    createRoom(3L, "주제3", 4, 0)
            );
            given(chattingRoomRepository.findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(null), any(PageRequest.class)))
                    .willReturn(rooms);

            // when
            ChatRoomListResponse response = chatRoomQueryService.getChatRooms(null, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.pageInfo().hasNext()).isTrue();
            assertThat(response.pageInfo().nextCursorId()).isEqualTo(4L);
            assertThat(response.pageInfo().size()).isEqualTo(2);
        }

        @Test
        @DisplayName("다음 페이지가 없으면 hasNext=false, nextCursorId는 null이다")
        void getChatRooms_hasNext_false() {
            // given
            int size = 10;
            List<ChattingRoom> rooms = List.of(
                    createRoom(2L, "주제2", 4, 1),
                    createRoom(1L, "주제1", 6, 3)
            );
            given(chattingRoomRepository.findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(null), any(PageRequest.class)))
                    .willReturn(rooms);

            // when
            ChatRoomListResponse response = chatRoomQueryService.getChatRooms(null, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.pageInfo().hasNext()).isFalse();
            assertThat(response.pageInfo().nextCursorId()).isNull();
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환한다")
        void getChatRooms_emptyResult() {
            // given
            given(chattingRoomRepository.findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(null), any(PageRequest.class)))
                    .willReturn(Collections.emptyList());

            // when
            ChatRoomListResponse response = chatRoomQueryService.getChatRooms(null, 10);

            // then
            assertThat(response.items()).isEmpty();
            assertThat(response.pageInfo().hasNext()).isFalse();
            assertThat(response.pageInfo().nextCursorId()).isNull();
        }

        @Test
        @DisplayName("cursorId가 주어지면 해당 id 이전의 방만 조회한다")
        void getChatRooms_withCursorId() {
            // given
            Long cursorId = 5L;
            int size = 10;
            List<ChattingRoom> rooms = List.of(
                    createRoom(4L, "주제4", 4, 1),
                    createRoom(3L, "주제3", 6, 2)
            );
            given(chattingRoomRepository.findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(cursorId), any(PageRequest.class)))
                    .willReturn(rooms);

            // when
            ChatRoomListResponse response = chatRoomQueryService.getChatRooms(cursorId, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items().getFirst().roomId()).isEqualTo(4L);
            then(chattingRoomRepository).should().findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(cursorId), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("대기실 조회")
    class GetWaitingRoom {

        @Test
        @DisplayName("대기실 조회에 성공하면 WaitingRoomResponse를 반환한다")
        void getWaitingRoom_success() {
            // given
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);

            ChattingRoomMember host = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(USER_ID).nickname("방장")
                    .profileImageUrl("http://host.url")
                    .role(MemberRole.HOST).position(Position.AGREE).build();

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(host));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(host));

            // when
            WaitingRoomResponse response = chatRoomQueryService.getWaitingRoom(ROOM_ID, USER_ID);

            // then
            assertThat(response.roomId()).isEqualTo(ROOM_ID);
            assertThat(response.agreeCount()).isEqualTo(1);
            assertThat(response.disagreeCount()).isZero();
            assertThat(response.maxPerPosition()).isEqualTo(2);
            assertThat(response.members()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void getWaitingRoom_roomNotFound() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getWaitingRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void getWaitingRoom_memberNotFound() {
            // given
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getWaitingRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("채팅방 상세 조회")
    class GetChatRoomDetail {

        private ChattingRoom createChattingRoom() {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, Long userId,
                                                 Position position) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(userId).nickname("닉네임")
                    .profileImageUrl("http://profile.url")
                    .role(MemberRole.PARTICIPANT).position(position).build();
            ReflectionTestUtils.setField(member, "status", MemberStatus.JOINED);
            return member;
        }

        private RoomRound createActiveRound(ChattingRoom room, int roundNumber) {
            RoomRound round = RoomRound.builder()
                    .chattingRoom(room).roundNumber(roundNumber).build();
            ReflectionTestUtils.setField(round, "id", 100L);
            return round;
        }

        @Test
        @DisplayName("CHATTING 상태의 방을 조회하면 멤버와 라운드 정보를 반환한다")
        void getChatRoomDetail_success() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember agreeMember = createMember(room, USER_ID, Position.AGREE);
            ChattingRoomMember disagreeMember = createMember(room, 2L, Position.DISAGREE);
            RoomRound activeRound = createActiveRound(room, 1);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(agreeMember));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(agreeMember, disagreeMember));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(activeRound));
            given(imageUrlResolver.toUrl("http://profile.url")).willReturn("http://profile.url");

            // when
            ChatRoomStartResponse response = chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID);

            // then
            assertThat(response.agreeMembers()).hasSize(1);
            assertThat(response.disagreeMembers()).hasSize(1);
            assertThat(response.currentRound()).isEqualTo(1);
            assertThat(response.startedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void getChatRoomDetail_roomNotFound() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("WAITING 상태이면 CHAT_ROOM_NOT_CHATTING 예외가 발생한다")
        void getChatRoomDetail_notChatting_waiting() {
            // given
            ChattingRoom room = createChattingRoom();
            ReflectionTestUtils.setField(room, "status", RoomStatus.WAITING);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("ENDED 상태이면 CHAT_ROOM_NOT_CHATTING 예외가 발생한다")
        void getChatRoomDetail_notChatting_ended() {
            // given
            ChattingRoom room = createChattingRoom();
            ReflectionTestUtils.setField(room, "status", RoomStatus.ENDED);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void getChatRoomDetail_memberNotFound() {
            // given
            ChattingRoom room = createChattingRoom();
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("활성 라운드가 없으면 CHAT_ROOM_ROUND_NOT_FOUND 예외가 발생한다")
        void getChatRoomDetail_roundNotFound() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember member = createMember(room, USER_ID, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(member));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomQueryService.getChatRoomDetail(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND);
        }
    }
}
