package com.example.doktoribackend.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {

    public record SessionInfo(Long userId, Long roomId) {}

    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRoomSessions = new ConcurrentHashMap<>();

    public void register(String sessionId, Long userId, Long roomId) {
        sessionMap.put(sessionId, new SessionInfo(userId, roomId));
        String key = userId + ":" + roomId;
        userRoomSessions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public Optional<SessionInfo> unregister(String sessionId) {
        SessionInfo info = sessionMap.remove(sessionId);
        if (info == null) {
            return Optional.empty();
        }

        String key = info.userId() + ":" + info.roomId();
        Set<String> sessions = userRoomSessions.get(key);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userRoomSessions.remove(key);
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }

    public void removeAllForRoom(Long roomId) {
        sessionMap.entrySet().removeIf(entry -> {
            if (entry.getValue().roomId().equals(roomId)) {
                String key = entry.getValue().userId() + ":" + roomId;
                userRoomSessions.remove(key);
                return true;
            }
            return false;
        });
    }
}
