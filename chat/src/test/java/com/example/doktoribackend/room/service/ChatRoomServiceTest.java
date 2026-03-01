package com.example.doktoribackend.room.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.book.service.BookService;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.quiz.service.QuizService;
import com.example.doktoribackend.summary.service.RoundSummaryService;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberRole;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.ChatStartMemberItem;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import com.example.doktoribackend.user.domain.UserInfo;
import com.example.doktoribackend.user.repository.UserInfoRepository;
import com.example.doktoribackend.vote.service.VoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
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
    private RoomRoundRepository roomRoundRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private com.example.doktoribackend.config.WebSocketSessionRegistry sessionRegistry;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private VoteService voteService;

    @Mock
    private BookService bookService;

    @Mock
    private QuizService quizService;

    @Mock
    private ChatRoomEventPublisher chatRoomEventPublisher;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ChatRoomQueryService chatRoomQueryService;

    @Mock
    private RoundSummaryService roundSummaryService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final String TEST_ISBN = "9781234567890";

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));
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
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(bookRepository.getReferenceById(any())).willReturn(createTestBook());
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
        @DisplayName("생성된 방에 Quiz가 저장된다")
        void createChatRoom_quizCreated() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(bookRepository.getReferenceById(any())).willReturn(createTestBook());
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

            // when
            chatRoomService.createChatRoom(USER_ID, request);

            // then
            then(quizService).should().createQuiz(any(ChattingRoom.class), eq(request.quiz()));
        }

        @Test
        @DisplayName("생성 시 HOST 역할의 멤버가 저장된다")
        void createChatRoom_hostMemberSaved() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(bookRepository.getReferenceById(any())).willReturn(createTestBook());
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    eq(USER_ID), any())).willReturn(false);
            given(chattingRoomRepository.save(any(ChattingRoom.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            stubUserInfo();

            org.mockito.ArgumentCaptor<ChattingRoomMember> memberCaptor =
                    org.mockito.ArgumentCaptor.forClass(ChattingRoomMember.class);

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
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(bookRepository.getReferenceById(any())).willReturn(createTestBook());
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
        @DisplayName("이미 WAITING, JOINED 또는 DISCONNECTED 상태로 참여 중이면 CHAT_ROOM_ALREADY_JOINED 예외가 발생한다")
        void alreadyJoined_throwsException() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(
                    USER_ID, List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatRoomService.createChatRoom(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_JOINED);

            then(chattingRoomRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("LEFT 상태만 있으면 새 채팅방을 생성할 수 있다")
        void leftOnly_canCreateNewRoom() {
            // given
            ChatRoomCreateRequest request = createValidRequest(4);
            given(bookService.resolveBook(TEST_ISBN)).willReturn(createTestBook());
            given(bookRepository.getReferenceById(any())).willReturn(createTestBook());
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
            assertThat(room.getCurrentMemberCount()).isZero();
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

        private ChattingRoom createWaitingRoom(int currentMemberCount) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제")
                    .description("설명")
                    .capacity(4)
                    .build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.WAITING);
            ReflectionTestUtils.setField(room, "currentMemberCount", currentMemberCount);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, MemberStatus status, Position position) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room)
                    .userId(USER_ID)
                    .role(MemberRole.PARTICIPANT)
                    .position(position)
                    .build();
            ReflectionTestUtils.setField(member, "status", status);
            return member;
        }

        @Test
        @DisplayName("유효한 요청으로 채팅방에 참여하면 WaitingRoomResponse를 반환한다")
        void joinChatRoom_success() {
            // given
            ChattingRoom room = createWaitingRoom(1);
            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
                    eq(ROOM_ID), eq(Position.AGREE), any())).willReturn(0);
            stubUserInfo();

            WaitingRoomResponse expectedResponse = new WaitingRoomResponse(ROOM_ID, 1, 1, 2, List.of());
            given(chatRoomQueryService.buildWaitingRoomResponse(room)).willReturn(expectedResponse);

            // when
            WaitingRoomResponse response = chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request);

            // then
            assertThat(response.roomId()).isEqualTo(ROOM_ID);
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
        @DisplayName("다른 방에 이미 참여 중이면 CHAT_ROOM_ALREADY_JOINED 예외가 발생한다")
        void joinChatRoom_alreadyJoinedOtherRoom() {
            // given
            ChattingRoom room = createWaitingRoom(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
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
        @DisplayName("이미 이 방에서 WAITING 상태이면 현재 대기실 상태를 반환한다")
        void joinChatRoom_alreadyWaiting_returnsCurrentState() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            ChattingRoomMember member = createMember(room, MemberStatus.WAITING, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));

            WaitingRoomResponse expectedResponse = new WaitingRoomResponse(ROOM_ID, 1, 1, 2, List.of());
            given(chatRoomQueryService.buildWaitingRoomResponse(room)).willReturn(expectedResponse);

            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when
            WaitingRoomResponse response = chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request);

            // then
            assertThat(response.roomId()).isEqualTo(ROOM_ID);
            then(chattingRoomMemberRepository).should(never()).save(any());
            assertThat(room.getCurrentMemberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("이 방에서 나갔다가 재입장하면 WAITING 상태로 복귀하고 인원수가 증가한다")
        void joinChatRoom_rejoin_success() {
            // given
            ChattingRoom room = createWaitingRoom(1);
            ChattingRoomMember member = createMember(room, MemberStatus.LEFT, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
                    eq(ROOM_ID), eq(Position.AGREE), any())).willReturn(0);

            WaitingRoomResponse expectedResponse = new WaitingRoomResponse(ROOM_ID, 1, 1, 2, List.of());
            given(chatRoomQueryService.buildWaitingRoomResponse(room)).willReturn(expectedResponse);

            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when
            WaitingRoomResponse response = chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request);

            // then
            assertThat(response.roomId()).isEqualTo(ROOM_ID);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WAITING);
            assertThat(room.getCurrentMemberCount()).isEqualTo(2);
            then(chattingRoomMemberRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("재입장 시 퀴즈 오답이면 CHAT_ROOM_QUIZ_WRONG_ANSWER 예외가 발생한다")
        void joinChatRoom_rejoin_wrongQuiz() {
            // given
            ChattingRoom room = createWaitingRoom(1);
            ChattingRoomMember member = createMember(room, MemberStatus.LEFT, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
            org.mockito.BDDMockito.willThrow(new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER))
                    .given(quizService).validateQuizAnswer(ROOM_ID, 2);

            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 2);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER);
        }

        @Test
        @DisplayName("재입장 시 정원이 가득 차면 CHAT_ROOM_FULL 예외가 발생한다")
        void joinChatRoom_rejoin_roomFull() {
            // given
            ChattingRoom room = createWaitingRoom(4);
            ChattingRoomMember member = createMember(room, MemberStatus.LEFT, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));

            ChatRoomJoinRequest request = new ChatRoomJoinRequest(Position.AGREE, 1);

            // when & then
            assertThatThrownBy(() -> chatRoomService.joinChatRoom(ROOM_ID, USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_FULL);
        }

        @Test
        @DisplayName("재입장 시 포지션이 가득 차면 CHAT_ROOM_POSITION_FULL 예외가 발생한다")
        void joinChatRoom_rejoin_positionFull() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            ChattingRoomMember member = createMember(room, MemberStatus.LEFT, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(member));
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
        @DisplayName("퀴즈 오답이면 CHAT_ROOM_QUIZ_WRONG_ANSWER 예외가 발생한다")
        void joinChatRoom_quizWrongAnswer() {
            // given
            ChattingRoom room = createWaitingRoom(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
            given(chattingRoomMemberRepository.existsByUserIdAndStatusIn(eq(USER_ID), any()))
                    .willReturn(false);
            org.mockito.BDDMockito.willThrow(new BusinessException(ErrorCode.CHAT_ROOM_QUIZ_WRONG_ANSWER))
                    .given(quizService).validateQuizAnswer(ROOM_ID, 2);
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
            ChattingRoom room = createWaitingRoom(4);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
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
            ChattingRoom room = createWaitingRoom(2);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
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
            ChattingRoom room = createWaitingRoom(1);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());
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
    @DisplayName("채팅 시작")
    class StartChatRoom {

        private ChattingRoom createWaitingRoom(int currentMemberCount) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.WAITING);
            ReflectionTestUtils.setField(room, "currentMemberCount", currentMemberCount);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, Long userId,
                                                 MemberRole role, Position position) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(userId).nickname("닉네임")
                    .profileImageUrl("http://profile.url")
                    .role(role).position(position).build();
            ReflectionTestUtils.setField(member, "status", MemberStatus.WAITING);
            return member;
        }

        @Test
        @DisplayName("HOST가 시작하면 방 CHATTING, 멤버 JOINED, RoomRound 생성, 응답에 멤버/라운드 정보 포함")
        void startChatRoom_success() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
            ChattingRoomMember participant = createMember(room, 2L, MemberRole.PARTICIPANT, Position.DISAGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(host));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(
                    ROOM_ID, List.of(MemberStatus.WAITING)))
                    .willReturn(List.of(host, participant));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(
                    eq(ROOM_ID), eq(List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED))))
                    .willReturn(List.of(host, participant));
            given(chatRoomQueryService.toStartMemberItem(any())).willAnswer(invocation -> {
                ChattingRoomMember m = invocation.getArgument(0);
                return new ChatStartMemberItem(m.getUserId(), m.getNickname(), m.getProfileImageUrl());
            });

            // when
            ChatRoomStartResponse response = chatRoomService.startChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.CHATTING);
            assertThat(host.getStatus()).isEqualTo(MemberStatus.JOINED);
            assertThat(participant.getStatus()).isEqualTo(MemberStatus.JOINED);
            assertThat(room.getRounds()).hasSize(1);
            assertThat(room.getRounds().getFirst().getRoundNumber()).isEqualTo(1);

            assertThat(response.agreeMembers()).hasSize(1);
            assertThat(response.agreeMembers().getFirst().nickname()).isEqualTo("닉네임");
            assertThat(response.disagreeMembers()).hasSize(1);
            assertThat(response.disagreeMembers().getFirst().nickname()).isEqualTo("닉네임");
            assertThat(response.currentRound()).isEqualTo(1);
            assertThat(response.startedAt()).isNotNull();
        }

        @Test
        @DisplayName("방이 존재하지 않으면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void startChatRoom_roomNotFound() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomService.startChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("WAITING 상태가 아니면 CHAT_ROOM_NOT_WAITING 예외가 발생한다")
        void startChatRoom_notWaiting() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> chatRoomService.startChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_WAITING);
        }

        @Test
        @DisplayName("멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void startChatRoom_memberNotFound() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatRoomService.startChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("HOST가 아니면 CHAT_ROOM_NOT_HOST 예외가 발생한다")
        void startChatRoom_notHost() {
            // given
            ChattingRoom room = createWaitingRoom(2);
            ChattingRoomMember participant = createMember(room, USER_ID, MemberRole.PARTICIPANT, Position.AGREE);

            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
                    .willReturn(Optional.of(participant));

            // when & then
            assertThatThrownBy(() -> chatRoomService.startChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        // TODO: 테스트 후 주석 해제
        // @Test
        // @DisplayName("상대 포지션에 멤버가 없으면 CHAT_ROOM_INSUFFICIENT_MEMBERS 예외가 발생한다")
        // void startChatRoom_insufficientMembers() {
        //     // given
        //     ChattingRoom room = createWaitingRoom(1);
        //     ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
        //
        //     given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        //     given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, USER_ID))
        //             .willReturn(Optional.of(host));
        //     given(chattingRoomMemberRepository.countByChattingRoomIdAndPositionAndStatusIn(
        //             eq(ROOM_ID), eq(Position.DISAGREE), any())).willReturn(0);
        //
        //     // when & then
        //     assertThatThrownBy(() -> chatRoomService.startChatRoom(ROOM_ID, USER_ID))
        //             .isInstanceOf(BusinessException.class)
        //             .extracting(e -> ((BusinessException) e).getErrorCode())
        //             .isEqualTo(ErrorCode.CHAT_ROOM_INSUFFICIENT_MEMBERS);
        // }
    }

    @Nested
    @DisplayName("다음 라운드 전환")
    class NextRound {

        private ChattingRoom createChattingRoom() {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, Long userId,
                                                 MemberRole role, Position position) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(userId).nickname("닉네임")
                    .profileImageUrl("http://profile.url")
                    .role(role).position(position).build();
            ReflectionTestUtils.setField(member, "status", MemberStatus.JOINED);
            return member;
        }

        private RoomRound createActiveRound(ChattingRoom room, int roundNumber) {
            return RoomRound.builder()
                    .chattingRoom(room).roundNumber(roundNumber).build();
        }

        @Test
        @DisplayName("HOST가 1라운드에서 다음 라운드로 전환하면 2라운드가 생성된다")
        void nextRound_success() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
            RoomRound activeRound = createActiveRound(room, 1);

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, host));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(activeRound));

            // when
            chatRoomService.nextRound(ROOM_ID, USER_ID);

            // then
            assertThat(activeRound.getEndedAt()).isNotNull();
            assertThat(room.getRounds()).hasSize(1);
            assertThat(room.getRounds().getFirst().getRoundNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void nextRound_roomNotFound() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> chatRoomService.nextRound(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("CHATTING 상태가 아니면 CHAT_ROOM_NOT_CHATTING 예외가 발생한다")
        void nextRound_notChatting() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING));

            // when & then
            assertThatThrownBy(() -> chatRoomService.nextRound(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void nextRound_memberNotFound() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> chatRoomService.nextRound(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("HOST가 아니면 CHAT_ROOM_NOT_HOST 예외가 발생한다")
        void nextRound_notHost() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember participant = createMember(room, USER_ID, MemberRole.PARTICIPANT, Position.AGREE);

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, participant));

            // when & then
            assertThatThrownBy(() -> chatRoomService.nextRound(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_HOST);
        }

        @Test
        @DisplayName("3라운드에서 다음 라운드 시도 시 CHAT_ROOM_MAX_ROUND_REACHED 예외가 발생한다")
        void nextRound_maxRoundReached() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
            RoomRound activeRound = createActiveRound(room, 3);

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, host));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(activeRound));

            // when & then
            assertThatThrownBy(() -> chatRoomService.nextRound(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MAX_ROUND_REACHED);
        }
    }

    @Nested
    @DisplayName("채팅방 종료")
    class EndChatRoom {

        private ChattingRoom createChattingRoom() {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).build();
            ReflectionTestUtils.setField(room, "id", ROOM_ID);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);
            return room;
        }

        private ChattingRoomMember createMember(ChattingRoom room, Long userId,
                                                 MemberRole role, Position position) {
            ChattingRoomMember member = ChattingRoomMember.builder()
                    .chattingRoom(room).userId(userId).nickname("닉네임")
                    .profileImageUrl("http://profile.url")
                    .role(role).position(position).build();
            ReflectionTestUtils.setField(member, "status", MemberStatus.JOINED);
            return member;
        }

        @Test
        @DisplayName("HOST가 종료하면 방 ENDED, 활성 멤버 LEFT, 활성 라운드 종료된다")
        void endChatRoom_success() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
            ChattingRoomMember participant = createMember(room, 2L, MemberRole.PARTICIPANT, Position.DISAGREE);
            RoomRound activeRound = RoomRound.builder().chattingRoom(room).roundNumber(3).build();

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, host));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(activeRound));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(ROOM_ID), any()))
                    .willReturn(List.of(host, participant));

            // when
            chatRoomService.endChatRoom(ROOM_ID, USER_ID);

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.ENDED);
            assertThat(activeRound.getEndedAt()).isNotNull();
            assertThat(host.getStatus()).isEqualTo(MemberStatus.LEFT);
            assertThat(participant.getStatus()).isEqualTo(MemberStatus.LEFT);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외가 발생한다")
        void endChatRoom_roomNotFound() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> chatRoomService.endChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("CHATTING 상태가 아니면 CHAT_ROOM_NOT_CHATTING 예외가 발생한다")
        void endChatRoom_notChatting() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING));

            // when & then
            assertThatThrownBy(() -> chatRoomService.endChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외가 발생한다")
        void endChatRoom_memberNotFound() {
            // given
            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willThrow(new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> chatRoomService.endChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("마지막 라운드가 아니면 CHAT_ROOM_NOT_LAST_ROUND 예외가 발생한다")
        void endChatRoom_notLastRound() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember host = createMember(room, USER_ID, MemberRole.HOST, Position.AGREE);
            RoomRound activeRound = RoomRound.builder().chattingRoom(room).roundNumber(2).build();

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, host));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(activeRound));

            // when & then
            assertThatThrownBy(() -> chatRoomService.endChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_LAST_ROUND);
        }

        @Test
        @DisplayName("HOST가 아니면 CHAT_ROOM_NOT_HOST 예외가 발생한다")
        void endChatRoom_notHost() {
            // given
            ChattingRoom room = createChattingRoom();
            ChattingRoomMember participant = createMember(room, USER_ID, MemberRole.PARTICIPANT, Position.AGREE);

            given(chatRoomQueryService.findChattingRoomAndMember(ROOM_ID, USER_ID))
                    .willReturn(new ChatRoomQueryService.ChattingRoomAndMember(room, participant));

            // when & then
            assertThatThrownBy(() -> chatRoomService.endChatRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_HOST);
        }
    }

    @Nested
    @DisplayName("만료 채팅방 자동 종료")
    class EndExpiredChatRooms {

        private ChattingRoom createChattingRoom(Long id, int duration) {
            ChattingRoom room = ChattingRoom.builder()
                    .topic("주제").description("설명").capacity(4).duration(duration).build();
            ReflectionTestUtils.setField(room, "id", id);
            ReflectionTestUtils.setField(room, "status", RoomStatus.CHATTING);
            return room;
        }

        @Test
        @DisplayName("duration이 지난 채팅방은 자동 종료된다")
        void endExpiredChatRooms_endsExpired() {
            // given
            ChattingRoom expiredRoom = createChattingRoom(1L, 30);

            given(chattingRoomRepository.findExpiredChattingRooms(any(java.time.LocalDateTime.class)))
                    .willReturn(List.of(expiredRoom));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(1L))
                    .willReturn(Optional.empty());
            given(chattingRoomMemberRepository.findByChattingRoomIdAndStatusIn(eq(1L), any()))
                    .willReturn(Collections.emptyList());

            // when
            chatRoomService.endExpiredChatRooms();

            // then
            assertThat(expiredRoom.getStatus()).isEqualTo(RoomStatus.ENDED);
        }

        @Test
        @DisplayName("duration이 지나지 않은 채팅방은 종료되지 않는다")
        void endExpiredChatRooms_skipsActive() {
            // given
            given(chattingRoomRepository.findExpiredChattingRooms(any(java.time.LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            chatRoomService.endExpiredChatRooms();

            // then
            then(chattingRoomRepository).should(never()).findById(any());
        }
    }
}
