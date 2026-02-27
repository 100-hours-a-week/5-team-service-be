package com.example.doktoribackend.config;

import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Nullable
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String sessionId = accessor.getSessionId();

        try {
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

            return message;
        } catch (MessageDeliveryException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[WebSocket] 인증 실패 - sessionId: {}, reason: {}", sessionId, ex.getMessage());
            throw new MessageDeliveryException("인증에 실패했습니다: " + ex.getMessage());
        }
    }
}
