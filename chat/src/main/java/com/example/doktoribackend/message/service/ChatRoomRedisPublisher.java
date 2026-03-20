package com.example.doktoribackend.message.service;

import com.example.doktoribackend.message.dto.ChatRoomBroadcastEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomRedisPublisher {

    private static final String CHANNEL_PREFIX = "chat-room:broadcast:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long roomId, Object broadcastData) {
        try {
            String payload = objectMapper.writeValueAsString(broadcastData);
            ChatRoomBroadcastEvent event = new ChatRoomBroadcastEvent(roomId, payload);
            String message = objectMapper.writeValueAsString(event);

            stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + roomId, message);
            log.info("[Redis Pub] 채팅방 이벤트 발행: roomId={}", roomId);
        } catch (JsonProcessingException e) {
            log.error("[Redis Pub] 직렬화 실패: roomId={}", roomId, e);
        }
    }
}
