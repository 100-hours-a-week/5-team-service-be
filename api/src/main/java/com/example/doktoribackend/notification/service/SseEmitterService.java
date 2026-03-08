package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;
    private static final String SSE_CHANNEL_PREFIX = "notification:sse:";
    private static final String SSE_CONNECTED_PREFIX = "notification:sse:connected:";

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SseEmitter subscribe(Long userId) {
        SseEmitter existingEmitter = emitters.remove(userId);
        if (existingEmitter != null) {
            existingEmitter.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);
        stringRedisTemplate.opsForValue().set(
                SSE_CONNECTED_PREFIX + userId, "1",
                Duration.ofMillis(SSE_TIMEOUT));

        Runnable removeConnected = () -> {
            emitters.remove(userId);
            stringRedisTemplate.delete(SSE_CONNECTED_PREFIX + userId);
        };
        emitter.onCompletion(removeConnected);
        emitter.onTimeout(removeConnected);
        emitter.onError(e -> removeConnected.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            log.error("Failed to send SSE connect event", e);
            emitters.remove(userId);
        }
        return emitter;
    }

    public void sendToUsers(List<Long> userIds, SseNotificationEvent event) {
        List<Long> connectedIds = userIds.stream()
                .filter(id -> stringRedisTemplate.hasKey(SSE_CONNECTED_PREFIX + id))
                .toList();
        if (connectedIds.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            for (Long userId : connectedIds) {
                stringRedisTemplate.convertAndSend(SSE_CHANNEL_PREFIX + userId, payload);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE event for Redis publish", e);
        }
    }

    public List<Long> filterSseDisconnectedUsers(List<Long> userIds) {
        return userIds.stream()
                .filter(id -> !stringRedisTemplate.hasKey(SSE_CONNECTED_PREFIX + id))
                .toList();
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        List<Long> deadUserIds = new ArrayList<>();
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(""));
            } catch (IOException e) {
                deadUserIds.add(userId);
            }
        });
        deadUserIds.forEach(userId -> {
            emitters.remove(userId);
            stringRedisTemplate.delete(SSE_CONNECTED_PREFIX + userId);
        });
    }

    public void deliverToLocal(Long userId, SseNotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(event));
        } catch (IOException e) {
            log.warn("Failed to send SSE notification to userId: {}", userId);
            emitters.remove(userId);
            stringRedisTemplate.delete(SSE_CONNECTED_PREFIX + userId);
        }
    }
}
