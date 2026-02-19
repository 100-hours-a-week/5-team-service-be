package com.example.doktoribackend.message.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.message.domain.Message;
import com.example.doktoribackend.message.domain.MessageType;
import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.dto.MessageSendRequest;
import com.example.doktoribackend.message.repository.MessageRepository;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChattingRoomRepository chattingRoomRepository;

    @Mock
    private ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Mock
    private RoomRoundRepository roomRoundRepository;

    @InjectMocks
    private MessageService messageService;

    private static final Long ROOM_ID = 10L;
    private static final Long SENDER_ID = 1L;
    private static final Long ROUND_ID = 100L;
    private static final String SENDER_NICKNAME = "테스터";
    private static final String CLIENT_MESSAGE_ID = "client-msg-001";
    private static final String TEXT_MESSAGE = "안녕하세요";

    private ChattingRoom createRoomWithStatus(RoomStatus status) {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제").description("설명").capacity(4).build();
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        ReflectionTestUtils.setField(room, "status", status);
        return room;
    }

    private ChattingRoomMember createMemberWithStatus(MemberStatus status) {
        ChattingRoomMember member = ChattingRoomMember.builder()
                .chattingRoom(createRoomWithStatus(RoomStatus.CHATTING))
                .userId(SENDER_ID)
                .build();
        ReflectionTestUtils.setField(member, "status", status);
        return member;
    }

    private RoomRound createActiveRound() {
        RoomRound round = RoomRound.builder()
                .chattingRoom(createRoomWithStatus(RoomStatus.CHATTING))
                .roundNumber(1)
                .build();
        ReflectionTestUtils.setField(round, "id", ROUND_ID);
        return round;
    }

    private static final String FILE_PATH = "chat/images/test-image.png";

    private MessageSendRequest createRequest() {
        return new MessageSendRequest(CLIENT_MESSAGE_ID, MessageType.TEXT, TEXT_MESSAGE, null);
    }

    private MessageSendRequest createFileRequest() {
        return new MessageSendRequest(CLIENT_MESSAGE_ID, MessageType.FILE, null, FILE_PATH);
    }

    private void stubSuccessScenario() {
        given(chattingRoomRepository.findById(ROOM_ID))
                .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
        given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                .willReturn(Optional.of(createMemberWithStatus(MemberStatus.JOINED)));
        given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                .willReturn(Optional.of(createActiveRound()));
        given(messageRepository.existsByRoomIdAndSenderIdAndClientMessageId(
                ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID))
                .willReturn(false);
    }

    @Nested
    @DisplayName("성공 케이스")
    class Success {

        @Test
        @DisplayName("유효한 요청으로 메시지를 전송하면 MessageResponse를 반환한다")
        void sendMessage_success() {
            // given
            stubSuccessScenario();

            // when
            MessageResponse response = messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, createRequest());

            // then
            assertThat(response).isNotNull();
            assertThat(response.senderId()).isEqualTo(SENDER_ID);
            assertThat(response.senderNickname()).isEqualTo(SENDER_NICKNAME);
            assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
            assertThat(response.textMessage()).isEqualTo(TEXT_MESSAGE);
        }

        @Test
        @DisplayName("FILE 타입 요청으로 메시지를 전송하면 filePath가 포함된 MessageResponse를 반환한다")
        void sendMessage_fileType_success() {
            // given
            stubSuccessScenario();

            // when
            MessageResponse response = messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, createFileRequest());

            // then
            assertThat(response).isNotNull();
            assertThat(response.senderId()).isEqualTo(SENDER_ID);
            assertThat(response.messageType()).isEqualTo(MessageType.FILE);
            assertThat(response.filePath()).isEqualTo(FILE_PATH);
            assertThat(response.textMessage()).isNull();
        }

        @Test
        @DisplayName("FILE 타입 메시지 저장 시 올바른 filePath가 사용된다")
        void sendMessage_fileType_correctFilePath() {
            // given
            stubSuccessScenario();
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            // when
            messageService.sendMessage(ROOM_ID, SENDER_ID, SENDER_NICKNAME, createFileRequest());

            // then
            then(messageRepository).should().save(messageCaptor.capture());
            Message saved = messageCaptor.getValue();
            assertThat(saved.getMessageType()).isEqualTo(MessageType.FILE);
            assertThat(saved.getFilePath()).isEqualTo(FILE_PATH);
            assertThat(saved.getTextMessage()).isNull();
        }

        @Test
        @DisplayName("메시지 저장 시 올바른 roundId가 사용된다")
        void sendMessage_correctRoundId() {
            // given
            stubSuccessScenario();
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            // when
            messageService.sendMessage(ROOM_ID, SENDER_ID, SENDER_NICKNAME, createRequest());

            // then
            then(messageRepository).should().save(messageCaptor.capture());
            assertThat(messageCaptor.getValue().getRoundId()).isEqualTo(ROUND_ID);
        }

        @Test
        @DisplayName("MessageRepository.save()가 정확히 1번 호출된다")
        void sendMessage_saveCalled() {
            // given
            stubSuccessScenario();

            // when
            messageService.sendMessage(ROOM_ID, SENDER_ID, SENDER_NICKNAME, createRequest());

            // then
            then(messageRepository).should().save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("채팅방 검증")
    class ChatRoomValidation {

        @Test
        @DisplayName("존재하지 않는 채팅방이면 CHAT_ROOM_NOT_FOUND 예외")
        void sendMessage_roomNotFound() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("WAITING 상태 채팅방이면 CHAT_ROOM_NOT_CHATTING 예외")
        void sendMessage_waitingRoom() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.WAITING)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("ENDED 상태 채팅방이면 CHAT_ROOM_NOT_CHATTING 예외")
        void sendMessage_endedRoom() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.ENDED)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        @Test
        @DisplayName("CANCELLED 상태 채팅방이면 CHAT_ROOM_NOT_CHATTING 예외")
        void sendMessage_cancelledRoom() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CANCELLED)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }
    }

    @Nested
    @DisplayName("멤버 검증")
    class MemberValidation {

        @Test
        @DisplayName("채팅방 멤버가 아니면 CHAT_ROOM_MEMBER_NOT_FOUND 예외")
        void sendMessage_memberNotFound() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("WAITING 상태 멤버면 CHAT_ROOM_MEMBER_NOT_FOUND 예외")
        void sendMessage_waitingMember() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.WAITING)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("DISCONNECTED 상태 멤버면 CHAT_ROOM_MEMBER_NOT_FOUND 예외")
        void sendMessage_disconnectedMember() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.DISCONNECTED)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("LEFT 상태 멤버면 CHAT_ROOM_MEMBER_NOT_FOUND 예외")
        void sendMessage_leftMember() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.LEFT)));

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("라운드 검증")
    class RoundValidation {

        @Test
        @DisplayName("진행 중인 라운드가 없으면 CHAT_ROOM_ROUND_NOT_FOUND 예외")
        void sendMessage_roundNotFound() {
            // given
            MessageSendRequest request = createRequest();
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.JOINED)));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("중복 메시지 검증")
    class DuplicateMessageValidation {

        @Test
        @DisplayName("동일한 clientMessageId면 null을 반환한다")
        void sendMessage_duplicate_returnsNull() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.JOINED)));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(createActiveRound()));
            given(messageRepository.existsByRoomIdAndSenderIdAndClientMessageId(
                    ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID))
                    .willReturn(true);

            // when
            MessageResponse response = messageService.sendMessage(
                    ROOM_ID, SENDER_ID, SENDER_NICKNAME, createRequest());

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("중복 메시지일 때 MessageRepository.save()가 호출되지 않는다")
        void sendMessage_duplicate_saveNotCalled() {
            // given
            given(chattingRoomRepository.findById(ROOM_ID))
                    .willReturn(Optional.of(createRoomWithStatus(RoomStatus.CHATTING)));
            given(chattingRoomMemberRepository.findByChattingRoomIdAndUserId(ROOM_ID, SENDER_ID))
                    .willReturn(Optional.of(createMemberWithStatus(MemberStatus.JOINED)));
            given(roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(ROOM_ID))
                    .willReturn(Optional.of(createActiveRound()));
            given(messageRepository.existsByRoomIdAndSenderIdAndClientMessageId(
                    ROOM_ID, SENDER_ID, CLIENT_MESSAGE_ID))
                    .willReturn(true);

            // when
            messageService.sendMessage(ROOM_ID, SENDER_ID, SENDER_NICKNAME, createRequest());

            // then
            then(messageRepository).should(never()).save(any());
        }
    }
}
