package com.example.doktoribackend.config;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.CustomException;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
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
    }
}
