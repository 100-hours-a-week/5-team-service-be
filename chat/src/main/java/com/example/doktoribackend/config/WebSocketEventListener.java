package com.example.doktoribackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        boolean authenticated = accessor.getUser() != null;
        log.info("[WebSocket] 연결 - sessionId: {}, authenticated: {}",
                accessor.getSessionId(), authenticated);
    }

    @EventListener
    public void handleWebSocketDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        CloseStatus closeStatus = event.getCloseStatus();
        log.info("[WebSocket] 해제 - sessionId: {}, closeStatus: {}",
                accessor.getSessionId(), closeStatus.getCode());
    }
}
