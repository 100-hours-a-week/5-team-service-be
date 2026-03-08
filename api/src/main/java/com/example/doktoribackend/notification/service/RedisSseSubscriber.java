package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSseSubscriber implements MessageListener {

    private static final String CHANNEL_PREFIX = "notification:sse:";

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            Long userId = Long.parseLong(channel.substring(CHANNEL_PREFIX.length()));
            SseNotificationEvent event = objectMapper.readValue(message.getBody(), SseNotificationEvent.class);
            sseEmitterService.deliverToLocal(userId, event);
        } catch (Exception e) {
            log.error("Failed to process Redis SSE message", e);
        }
    }
}
