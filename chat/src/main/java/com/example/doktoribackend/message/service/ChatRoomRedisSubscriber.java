package com.example.doktoribackend.message.service;

import com.example.doktoribackend.message.dto.ChatRoomBroadcastEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            ChatRoomBroadcastEvent event = objectMapper.readValue(message.getBody(), ChatRoomBroadcastEvent.class);

            log.info("[Redis Sub] 채팅방 이벤트 수신: roomId={}", event.roomId());

            Object payload = objectMapper.readValue(event.payload(), Object.class);
            messagingTemplate.convertAndSend("/topic/chat-rooms/" + event.roomId(), payload);

            log.info("[Redis Sub] WebSocket 브로드캐스트 완료: roomId={}", event.roomId());
        } catch (Exception e) {
            log.error("[Redis Sub] 채팅방 이벤트 처리 실패", e);
        }
    }
}
