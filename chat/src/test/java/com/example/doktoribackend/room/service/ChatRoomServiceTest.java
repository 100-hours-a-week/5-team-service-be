package com.example.doktoribackend.room.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.client.KakaoBookClient;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.user.domain.UserInfo;
import com.example.doktoribackend.user.repository.UserInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private KakaoBookClient kakaoBookClient;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private WaitingRoomSseService waitingRoomSseService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final String TEST_ISBN = "9781234567890";

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Book createTestBook() {
        return Book.create(TEST_ISBN, "테스트 책", "저자", "출판사", "http://thumb.url", LocalDate.now());
    }

    private void stubUserInfo() {
        UserInfo userInfo = mock(UserInfo.class);
        given(userInfo.getNickname()).willReturn("테스터");
        given(userInfo.getProfileImagePath()).willReturn("http://profile.url");
        given(userInfoRepository.findById(USER_ID)).willReturn(Optional.of(userInfo));
    }

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
                "토론 주제", "주제 설명입니다", TEST_ISBN, capacity, Position.AGREE, quiz
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
            given(bookRepository.findByIsbn(TEST_ISBN)).willReturn(Optional.of(createTestBook()));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

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
            given(bookRepository.findByIsbn(TEST_ISBN)).willReturn(Optional.of(createTestBook()));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            stubUserInfo();

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
            given(bookRepository.findByIsbn(TEST_ISBN)).willReturn(Optional.of(createTestBook()));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

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
            given(bookRepository.findByIsbn(TEST_ISBN)).willReturn(Optional.of(createTestBook()));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

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
            given(bookRepository.findByIsbn(TEST_ISBN)).willReturn(Optional.of(createTestBook()));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

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
            assertThat(response.items().getFirst().roomId()).isEqualTo(4L);
            then(chattingRoomRepository).should().findByStatusWithCursor(
                    eq(RoomStatus.WAITING), eq(cursorId), any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("채팅방 나가기")
    class LeaveChatRoom {

        private static final Long ROOM_ID = 10L;

        private ChattingRoom createRoomWithStatus(RoomStatus status, int currentMemberCount) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제")
                    .description("설명")
                    .capacity(4)
                    .build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", status);
            ReflectionTestUtils.setField(room, "currentMemberCount", currentMemberCount);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, Long userId, MemberRole role, MemberStatus status) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room)
                    .userId(userId)
                    .role(role)
                    .position(Position.AGREE)
                    .build();
            ReflectionTestUtils.setField(member, "status", status);
            return member;
        }

        @Test
        @DisplayName("WAITING 상태의 방에서 일반 멤버가 나가면 멤버만 LEFT 처리된다")
        void leaveChatRoom_waitingMember_success() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.WAITING, 3);
            ChattingRoomMember member = createMember(room, USER_ID, MemberRole.PARTICIPANT, MemberStatus.WAITING);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(Collections.emptyList());

            // when
            chatRoomService.leaveChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(room.getCurrentMemberCount()).isEqualTo(2);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        }

        @Test
        @DisplayName("WAITING 상태의 방에서 방장이 나가면 방이 CANCELLED되고 전체 멤버가 LEFT 처리된다")
        void leaveChatRoom_waitingHost_cancelsRoom() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.WAITING, 3);
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, MemberStatus.WAITING);
            ChattingRoomMember member1 = createMember(room, 2L, MemberRole.PARTICIPANT, MemberStatus.WAITING);
            ChattingRoomMember member2 = createMember(room, 3L, MemberRole.PARTICIPANT, MemberStatus.WAITING);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(host));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(member1, member2));

            // when
            chatRoomService.leaveChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.CANCELLED);
            assertThat(host.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(member1.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(member2.getStatus()).isEqualTo(MemberStatus.LEFT);
        }

        @Test
        @DisplayName("CHATTING 상태의 방에서 나가면 해당 멤버만 LEFT 처리되고 방은 유지된다")
        void leaveChatRoom_chatting_memberOnly() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.CHATTING, 4);
            ChattingRoomMember member = createMember(room, USER_ID, MemberRole.PARTICIPANT, MemberStatus.JOINED);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(Collections.emptyList());

            // when
            chatRoomService.leaveChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(room.getCurrentMemberCount()).isEqualTo(3);
            assertThat(room.getStatus()).isEqualTo(RoomStatus.CHATTING);
        }

        @Test
        @DisplayName("DISCONNECTED 상태의 멤버도 나갈 수 있다")
        void leaveChatRoom_disconnectedMember_success() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.CHATTING, 3);
            ChattingRoomMember member = createMember(room, USER_ID, MemberRole.PARTICIPANT, MemberStatus.DISCONNECTED);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(Collections.emptyList());

            // when
            chatRoomService.leaveChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(room.getCurrentMemberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("ENDED 상태의 방에서는 나갈 수 없다")
        void leaveChatRoom_endedRoom_throws() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.ENDED, 2);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_ENDED);
        }

        @Test
        @DisplayName("CANCELLED 상태의 방에서는 나갈 수 없다")
        void leaveChatRoom_cancelledRoom_throws() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.CANCELLED, 0);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_ENDED);
        }

        @Test
        @DisplayName("이미 LEFT 상태인 멤버는 나갈 수 없다")
        void leaveChatRoom_alreadyLeft_throws() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.CHATTING, 2);
            ChattingRoomMember member = createMember(room, USER_ID, MemberRole.PARTICIPANT, MemberStatus.LEFT);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_LEFT);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void leaveChatRoom_roomNotFound_throws() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void leaveChatRoom_memberNotFound_throws() {
            // given
            ChattingRoom room = createRoomWithStatus(RoomStatus.WAITING, 2);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomService.leaveChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("채팅방 참여")
    class JoinChatRoom {

        private ChattingRoom createWaitingRoomWithQuiz(int currentMemberCount) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제")
                    .description("설명")
                    .capacity(4)
                    .build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.WAITING);
            ReflectionTestUtils.setField(room, "currentMemberCount", currentMemberCount);

            Quiz quiz = mock(Quiz.class);
            lenient().when(quiz.isCorrect(1)).thenReturn(true);
            ReflectionTestUtils.setField(room, "quiz", quiz);

            return room;
        }

        @Test
        @DisplayName("유효한 요청으로 채팅방에 참여하면 WaitingRoomResponse를 반환한다")
        void joinChatRoom_success() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(1);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
                    eq(ROOM_ID), eq(Position.AGREE), any())).willReturn(0);
            stubUserInfo();

            ChattingRoomMember host = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(99L).nickname("방장")
                    .profileImageUrl("http://host.url")
                    .role(MemberRole.HOST).position(Position.DISAGREE).build();
            ChattingRoomMember participant = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(USER_ID).nickname("테스터")
                    .profileImageUrl("http://profile.url")
                    .role(MemberRole.PARTICIPANT).position(Position.AGREE).build();
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(host, participant));

            // when
            WaitingRoomResponse response = chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).isEqualTo(ROOM_ID);
            assertThat(response.agreeCount()).isEqualTo(1);
            assertThat(response.disagreeCount()).isEqualTo(1);
            assertThat(response.maxPerPosition()).isEqualTo(2);
            assertThat(response.members()).hasSize(2);
            then(chattingRoomMemberRepository).should().save(any(ChattingRoomMember.class));
            assertThat(room.getCurrentMemberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void joinChatRoom_roomNotFound() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("WAITING 상태가 아닌 방이면 CHAT_ROOM_NOT_WAITING 예외가 발생한다")
        void joinChatRoom_notWaiting() {
            // given
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        @Test
        @DisplayName("이미 참여 중이면 CHAT_ROOM_ALREADY_JOINED 예외가 발생한다")
        void joinChatRoom_alreadyJoined() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(true);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        }

        @Test
        @DisplayName("퀴즈 오답이면 CHAT_ROOM_QUIZ_WRONG_ANSWER 예외가 발생한다")
        void joinChatRoom_quizWrongAnswer() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 2);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER);
        }

        @Test
        @DisplayName("정원이 가득 차면 CHAT_ROOM_FULL 예외가 발생한다")
        void joinChatRoom_roomFull() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(4);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_FULL);
        }

        @Test
        @DisplayName("해당 포지션이 가득 차면 CHAT_ROOM_POSITION_FULL 예외가 발생한다")
        void joinChatRoom_positionFull() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(2);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
                    eq(ROOM_ID), eq(Position.AGREE), any())).willReturn(2);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_POSITION_FULL);
        }

        @Test
        @DisplayName("사용자 정보가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void joinChatRoom_userNotFound() {
            // given
            ChattingRoom room = createWaitingRoomWithQuiz(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
                    eq(ROOM_ID), eq(Position.AGREE), any())).willReturn(0);
            given(userInfoRepository.findById(USER_ID)).willReturn(Optional.empty());
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
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
            WaitingRoomResponse response = chatRoomService.getWaitingRoom(ROOM_ID, USER_ID);

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
            assertThatThrownBy(() -> chatRoomService.getWaitingRoom(ROOM_ID, USER_ID))
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
            assertThatThrownBy(() -> chatRoomService.getWaitingRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }
    }
}
