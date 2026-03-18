package com.example.doktoribackend.room.service;

import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import com.example.doktoribackend.room.dto.WaitingRoomSseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomSseService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    private static final String CHANNEL_PREFIX = "waiting-room:sse:";

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SseEmitter subscribe(Long roomId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(roomId, emitter));
        emitter.onTimeout(() -> remove(roomId, emitter));
        emitter.onError(e -> remove(roomId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            remove(roomId, emitter);
        }

        return emitter;
    }

    public void broadcast(Long roomId, WaitingRoomResponse response) {
        publish(roomId, "waiting-room-update", objectMapper.valueToTree(response).toString());
    }

    public void broadcastCancelledAndClose(Long roomId) {
        publish(roomId, "room-cancelled", "방장이 나가 채팅방이 취소되었습니다.");
    }

    public void broadcastStartedAndClose(Long roomId, ChatRoomStartResponse response) {
        publish(roomId, "room-started", objectMapper.valueToTree(response).toString());
    }

    public void deliverToLocal(Long roomId, WaitingRoomSseEvent event) {
        boolean isTerminal = "room-cancelled".equals(event.name()) || "room-started".equals(event.name());
        List<SseEmitter> roomEmitters = isTerminal ? emitters.remove(roomId) : emitters.get(roomId);
        if (roomEmitters == null) {
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : roomEmitters) {
            try {
                emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
            } catch (IOException e) {
                dead.add(emitter);
            }
            if (isTerminal) {
                emitter.complete();
            }
        }

        if (!isTerminal) {
            dead.forEach(e -> remove(roomId, e));
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        emitters.forEach((roomId, roomEmitters) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : roomEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    dead.add(emitter);
                }
            }
            dead.forEach(e -> remove(roomId, e));
        });
    }

    private void publish(Long roomId, String name, String data) {
        try {
            String payload = objectMapper.writeValueAsString(new WaitingRoomSseEvent(name, data));
            stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + roomId, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WaitingRoomSseEvent for roomId: {}", roomId, e);
        }
    }

    private void remove(Long roomId, SseEmitter emitter) {
        List<SseEmitter> roomEmitters = emitters.get(roomId);
        if (roomEmitters != null) {
            roomEmitters.remove(emitter);
            if (roomEmitters.isEmpty()) {
                emitters.remove(roomId);
            }
        }
    }
}
