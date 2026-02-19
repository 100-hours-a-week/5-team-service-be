package com.example.doktoribackend.message.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.message.domain.Message;
import com.example.doktoribackend.message.domain.MessageType;
import com.example.doktoribackend.message.dto.MessageListResponse;
import com.example.doktoribackend.message.dto.MessageResponse;
import com.example.doktoribackend.message.dto.MessageSendRequest;
import com.example.doktoribackend.message.repository.MessageRepository;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.RoomRound;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.PageInfo;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import com.example.doktoribackend.room.repository.RoomRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final RoomRoundRepository roomRoundRepository;

    @Transactional(readOnly = true)
    public MessageListResponse getMessages(Long roomId, Long userId, Long cursorId, int size) {
        chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        List<Message> messages = messageRepository.findByRoomIdWithCursor(
                roomId, cursorId, PageRequest.of(0, size + 1));

        boolean hasNext = messages.size() > size;
        List<Message> content = hasNext ? messages.subList(0, size) : messages;

        List<ChattingRoomMember> members = chattingRoomMemberRepository
                .findByChattingRoomIdAndStatusIn(roomId,
                        List.of(MemberStatus.WAITING, MemberStatus.JOINED, MemberStatus.DISCONNECTED, MemberStatus.LEFT));
        Map<Long, String> nicknameMap = members.stream()
                .collect(Collectors.toMap(ChattingRoomMember::getUserId, ChattingRoomMember::getNickname, (a, b) -> a));

        List<MessageResponse> messageResponses = content.stream()
                .map(m -> MessageResponse.from(m, nicknameMap.getOrDefault(m.getSenderId(), "알 수 없음")))
                .toList();

        Long nextCursorId = hasNext ? content.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(nextCursorId, hasNext, size);

        return new MessageListResponse(messageResponses, pageInfo);
    }

    @Transactional
    public MessageResponse sendMessage(Long roomId, Long senderId, String senderNickname,
                                       MessageSendRequest request) {
        ChattingRoom room = chattingRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.CHATTING) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_CHATTING);
        }

        ChattingRoomMember member = chattingRoomMemberRepository
                .findByChattingRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));

        if (member.getStatus() != MemberStatus.JOINED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
        }

        RoomRound activeRound = roomRoundRepository.findByChattingRoomIdAndEndedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_ROUND_NOT_FOUND));

        if (messageRepository.existsByRoomIdAndSenderIdAndClientMessageId(
                roomId, senderId, request.clientMessageId())) {
            return null;
        }

        Message message = request.messageType() == MessageType.FILE
                ? Message.createFileMessage(
                        roomId, activeRound.getId(), senderId,
                        request.clientMessageId(), request.filePath())
                : Message.createTextMessage(
                        roomId, activeRound.getId(), senderId,
                        request.clientMessageId(), request.textMessage());

        messageRepository.save(message);

        return MessageResponse.from(message, senderNickname);
    }
}
