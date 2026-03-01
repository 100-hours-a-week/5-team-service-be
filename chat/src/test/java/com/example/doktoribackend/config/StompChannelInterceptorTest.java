package com.example.doktoribackend.config;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompChannelInterceptor interceptor;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final String NICKNAME = "tester";

    @Test
    @DisplayName("CONNECT 프레임이 아니면 메시지를 그대로 반환한다")
    void nonConnectCommandPassesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("CONNECT 시 유효한 토큰이면 인증 정보를 설정한다")
    void connectWithValidTokenSetsAuthentication() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + VALID_TOKEN);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        given(jwtTokenProvider.getUserIdFromAccessToken(VALID_TOKEN)).willReturn(USER_ID);
        given(jwtTokenProvider.getNicknameFromAccessToken(VALID_TOKEN)).willReturn(NICKNAME);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);

        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        Assertions.assertNotNull(auth);
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        assertThat(userDetails.getId()).isEqualTo(USER_ID);
        assertThat(userDetails.getNickname()).isEqualTo(NICKNAME);
    }

    @Test
    @DisplayName("CONNECT 시 Authorization 헤더가 없으면 MessageDeliveryException이 발생한다")
    void connectWithoutAuthHeaderThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("인증이 필요합니다");
    }

    @Test
    @DisplayName("CONNECT 시 Bearer 접두사가 없으면 MessageDeliveryException이 발생한다")
    void connectWithInvalidPrefixThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "InvalidPrefix " + VALID_TOKEN);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("인증이 필요합니다");
    }

    @Test
    @DisplayName("CONNECT 시 만료된 토큰이면 MessageDeliveryException이 발생한다")
    void connectWithExpiredTokenThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer expired.token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        given(jwtTokenProvider.getUserIdFromAccessToken("expired.token"))
                .willThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("토큰이 만료되었습니다");
    }
}
