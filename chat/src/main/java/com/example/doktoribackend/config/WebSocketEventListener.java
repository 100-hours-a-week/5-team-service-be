package com.example.doktoribackend.config;

import com.example.doktoribackend.room.service.ChatRoomConnectionService;
import com.example.doktoribackend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Pattern CHAT_ROOM_TOPIC_PATTERN =
            Pattern.compile("^/topic/chat-rooms/(\\d+)$");

    private final WebSocketSessionRegistry sessionRegistry;
    private final ChatRoomConnectionService connectionService;

    @EventListener
    public void handleWebSocketConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        boolean authenticated = accessor.getUser() != null;
        log.info("[WebSocket] 연결 - sessionId: {}, authenticated: {}",
                accessor.getSessionId(), authenticated);
    }

    @EventListener
    public void handleWebSocketSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = CHAT_ROOM_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long roomId = Long.parseLong(matcher.group(1));

        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth)) {
            return;
        }
        if (!(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return;
        }

        Long userId = userDetails.getId();
        String sessionId = accessor.getSessionId();

        sessionRegistry.register(sessionId, userId, roomId);
        connectionService.handleReconnect(roomId, userId);

        log.info("[WebSocket] SUBSCRIBE - sessionId: {}, roomId: {}", sessionId, roomId);
    }

    @EventListener
    public void handleWebSocketDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        CloseStatus closeStatus = event.getCloseStatus();
        log.info("[WebSocket] 해제 - sessionId: {}, closeStatus: {}",
                sessionId, closeStatus.getCode());

        sessionRegistry.unregister(sessionId).ifPresent(info -> {
            log.info("[WebSocket] 마지막 세션 해제 - roomId: {}", info.roomId());
            connectionService.handleDisconnect(info.roomId(), info.userId());
        });
    }
}
