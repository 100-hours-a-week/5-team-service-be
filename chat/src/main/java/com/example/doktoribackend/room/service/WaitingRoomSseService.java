package com.example.doktoribackend.room.service;

import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class WaitingRoomSseService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

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
        List<SseEmitter> roomEmitters = emitters.get(roomId);
        if (roomEmitters == null) {
            return;
        }

        for (SseEmitter emitter : roomEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("waiting-room-update")
                        .data(response));
            } catch (IOException e) {
                remove(roomId, emitter);
            }
        }
    }

    public void broadcastCancelledAndClose(Long roomId) {
        List<SseEmitter> roomEmitters = emitters.remove(roomId);
        if (roomEmitters == null) {
            return;
        }

        for (SseEmitter emitter : roomEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("room-cancelled")
                        .data("방장이 나가 채팅방이 취소되었습니다."));
                emitter.complete();
            } catch (IOException e) {
                emitter.complete();
            }
        }
    }

    public void broadcastStartedAndClose(Long roomId, ChatRoomStartResponse response) {
        List<SseEmitter> roomEmitters = emitters.remove(roomId);
        if (roomEmitters == null) {
            return;
        }

        for (SseEmitter emitter : roomEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("room-started")
                        .data(response));
                emitter.complete();
            } catch (IOException e) {
                emitter.complete();
            }
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
