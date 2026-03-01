package com.example.doktoribackend.room.service;

import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.RoomStatus;
import com.example.doktoribackend.room.dto.MemberStatusChangeEvent;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.room.repository.ChattingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomConnectionService {

    private final ChattingRoomRepository chattingRoomRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void handleDisconnect(Long roomId, Long userId) {
        ChattingRoom room = chattingRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.CHATTING) {
            return;
        }

        ChattingRoomMember member = chattingRoomMemberRepository
                .findByChattingRoomIdAndUserId(roomId, userId).orElse(null);
        if (member == null || member.getStatus() != MemberStatus.JOINED) {
            return;
        }

        member.disconnect();
        log.info("[Chat] 멤버 연결 해제 - roomId: {}", roomId);

        messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId,
                new MemberStatusChangeEvent(
                        "MEMBER_DISCONNECTED",
                        userId,
                        member.getNickname(),
                        MemberStatus.DISCONNECTED.name()));
    }

    @Transactional
    public void handleReconnect(Long roomId, Long userId) {
        ChattingRoom room = chattingRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getStatus() != RoomStatus.CHATTING) {
            return;
        }

        ChattingRoomMember member = chattingRoomMemberRepository
                .findByChattingRoomIdAndUserId(roomId, userId).orElse(null);
        if (member == null || member.getStatus() != MemberStatus.DISCONNECTED) {
            return;
        }

        member.join();
        log.info("[Chat] 멤버 재연결 - roomId: {}", roomId);

        messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId,
                new MemberStatusChangeEvent(
                        "MEMBER_RECONNECTED",
                        userId,
                        member.getNickname(),
                        MemberStatus.JOINED.name()));
    }
}
