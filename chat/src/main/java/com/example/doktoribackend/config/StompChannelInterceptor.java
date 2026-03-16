package com.example.doktoribackend.config;

import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final Counter connectSuccessCounter;
    private final Counter connectFailureCounter;

    public StompChannelInterceptor(JwtTokenProvider jwtTokenProvider, MeterRegistry meterRegistry) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.connectSuccessCounter = Counter.builder("chat.ws.connect")
                .tag("result", "success")
                .description("WebSocket STOMP CONNECT attempts")
                .register(meterRegistry);
        this.connectFailureCounter = Counter.builder("chat.ws.connect")
                .tag("result", "failure")
                .description("WebSocket STOMP CONNECT attempts")
                .register(meterRegistry);
    }

    @Override
    @Nullable
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        String sessionId = accessor.getSessionId();
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);

        try {
            if (accessor.getCommand() != StompCommand.CONNECT) {
                return message;
            }

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("[WebSocket] 인증 실패 - sessionId: {}, reason: Authorization 헤더 누락", sessionId);
                throw new MessageDeliveryException("인증이 필요합니다.");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            Long userId = jwtTokenProvider.getUserIdFromAccessToken(token);
            String nickname = jwtTokenProvider.getNicknameFromAccessToken(token);

            CustomUserDetails userDetails = CustomUserDetails.of(userId, nickname);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);
            connectSuccessCounter.increment();

            return message;
        } catch (MessageDeliveryException ex) {
            connectFailureCounter.increment();
            throw ex;
        } catch (Exception ex) {
            connectFailureCounter.increment();
            log.warn("[WebSocket] 인증 실패 - sessionId: {}, reason: {}", sessionId, ex.getMessage());
            throw new MessageDeliveryException("인증에 실패했습니다: " + ex.getMessage());
        }
    }

    @Override
    public void afterSendCompletion(@NonNull Message<?> message, @NonNull MessageChannel channel,
                                    boolean sent, Exception ex) {
        MDC.remove("traceId");
    }
}
