package com.example.doktoribackend.room.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private static final Long USER_ID = 1L;

    private ChatRoomCreateRequest createValidRequest(int capacity) {
        List<ChatRoomCreateRequest.QuizChoiceRequest> choices = List.of(
                new ChatRoomCreateRequest.QuizChoiceRequest(1, "선택지1"),
                new ChatRoomCreateRequest.QuizChoiceRequest(2, "선택지2"),
                new ChatRoomCreateRequest.QuizChoiceRequest(3, "선택지3"),
                new ChatRoomCreateRequest.QuizChoiceRequest(4, "선택지4")
        );

        ChatRoomCreateRequest.QuizRequest quiz = new ChatRoomCreateRequest.QuizRequest(
                "퀴즈 질문입니다", choices, 1
        );

        return new ChatRoomCreateRequest(
                "토론 주제", "주제 설명입니다", capacity, Position.AGREE, quiz
        );
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("유효한 요청으로 채팅방을 생성하면 roomId를 반환한다")
        void createChatRoom_success() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomCreateResponse response = chatRoomService.createChatRoom(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            then(chattingRoomRepository).should().save(any(ChattingRoom.class));
            then(chattingRoomMemberRepository).should().save(any(ChattingRoomMember.class));
        }

        @Test
        @DisplayName("생성된 방에 Quiz가 연결되고 QuizChoice 4개가 저장된다")
        void createChatRoom_quizCreated() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);

            ArgumentCaptor<ChattingRoom> roomCaptor = ArgumentCaptor.forClass(ChattingRoom.class);
            given(chattingRoomRepository.save(roomCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            chatRoomService.createChatRoom(USER_ID, request);

            // then
            ChattingRoom savedRoom = roomCaptor.getValue();
            assertThat(savedRoom.getQuiz()).isNotNull();
            assertThat(savedRoom.getQuiz().getQuestion()).isEqualTo("퀴즈 질문입니다");
            assertThat(savedRoom.getQuiz().getCorrectChoiceNumber()).isEqualTo(1);
            assertThat(savedRoom.getQuiz().getChoices()).hasSize(4);
        }

        @Test
        @DisplayName("생성 시 HOST 역할의 멤버가 저장된다")
        void createChatRoom_hostMemberSaved() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<ChattingRoomMember> memberCaptor =
                    ArgumentCaptor.forClass(ChattingRoomMember.class);

            // when
            chatRoomService.createChatRoom(USER_ID, request);

            // then
            then(chattingRoomMemberRepository).should().save(memberCaptor.capture());
            ChattingRoomMember savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRole()).isEqualTo(MemberRole.HOST);
            assertThat(savedMember.getUserId()).isEqualTo(USER_ID);
            assertThat(savedMember.getPosition()).isEqualTo(Position.AGREE);
        }
    }

    @Nested
    @DisplayName("capacity 검증")
    class CapacityValidation {

        @ParameterizedTest
        @ValueSource(ints = {2, 4, 6})
        @DisplayName("허용된 정원(2, 4, 6)이면 성공한다")
        void allowedCapacity_success(int capacity) {
            // given
            ChatRoomCreateRequest request = createValidRequest(capacity);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomCreateResponse response = chatRoomService.createChatRoom(USER_ID, request);

            // then
            assertThat(response).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 3, 5, 7, 8, 10, 100})
        @DisplayName("허용되지 않은 정원이면 INVALID_INPUT_VALUE 예외가 발생한다")
        void invalidCapacity_throwsException(int capacity) {
            // given
            ChatRoomCreateRequest request = createValidRequest(capacity);

            // when & then
            assertThatThrownBy(() -> chatRoomService.createChatRoom(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_INVALID_CAPACITY);

            then(chattingRoomRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("중복 참여 검증")
    class DuplicateJoinValidation {

        @Test
        @DisplayName("이미 WAITING 또는 JOINED 상태로 참여 중이면 CHAT_ROOM_ALREADY_JOINED 예외가 발생한다")
        void alreadyJoined_throwsException() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    USER_ID, List.of(MemberStatus.WAITING, MemberStatus.JOINED)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatRoomService.createChatRoom(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_JOINED);

            then(chattingRoomRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("LEFT 또는 DISCONNECTED 상태만 있으면 새 채팅방을 생성할 수 있다")
        void leftOrDisconnected_canCreateNewRoom() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatRoomCreateResponse response = chatRoomService.createChatRoom(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            then(chattingRoomRepository).should().save(any(ChattingRoom.class));
        }
    }

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
            ChatRoomListResponse response = chatRoomService.getChatRooms(null, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0).roomId()).isEqualTo(3L);
            assertThat(response.items().get(0).topic()).isEqualTo("주제3");
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
            ChatRoomListResponse response = chatRoomService.getChatRooms(null, size);

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
            ChatRoomListResponse response = chatRoomService.getChatRooms(null, size);

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
            ChatRoomListResponse response = chatRoomService.getChatRooms(null, 10);

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
            ChatRoomListResponse response = chatRoomService.getChatRooms(cursorId, size);

            // then
            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0).roomId()).isEqualTo(4L);
            then(chattingRoomRepository).should().findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(cursorId), any(PageRequest.class));
        }
    }
}
