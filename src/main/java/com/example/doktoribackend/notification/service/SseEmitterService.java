package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter existingEmitter = emitters.remove(userId);
        if (existingEmitter != null) {
            existingEmitter.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));

        emitter.onTimeout(() -> emitters.remove(userId));

        emitter.onError(e -> emitters.remove(userId));

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

    public void sendToUser(Long userId, SseNotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No active SSE connection");
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(event));
        } catch (IOException e) {
            log.warn("Failed to send SSE notification to userId: {}", userId);
            emitters.remove(userId);
        }
    }

    public void sendToUsers(List<Long> userIds, SseNotificationEvent event) {
        for (Long userId : userIds) {
            sendToUser(userId, event);
        }
    }
}
