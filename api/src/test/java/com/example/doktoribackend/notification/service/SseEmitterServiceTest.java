package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @Mock
    StringRedisTemplate stringRedisTemplate;

    SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("subscribe: SSE 연결을 생성하고 반환한다")
    void subscribe_createsEmitter() {
        SseEmitter emitter = sseEmitterService.subscribe(1L);

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    @DisplayName("subscribe: 기존 연결이 있으면 완료하고 새 연결을 생성한다")
    void subscribe_existingConnection_replacesIt() {
        SseEmitter firstEmitter = sseEmitterService.subscribe(1L);
        SseEmitter secondEmitter = sseEmitterService.subscribe(1L);

        assertThat(secondEmitter).isNotNull();
        assertThat(secondEmitter).isNotSameAs(firstEmitter);
    }

    @Test
    @DisplayName("deliverToLocal: 연결된 사용자에게 이벤트를 전송한다")
    void deliverToLocal_connectedUser_sendsEvent() {
        sseEmitterService.subscribe(1L);

        // when - 예외 없이 실행되어야 함
        sseEmitterService.deliverToLocal(1L, createEvent());
    }

    @Test
    @DisplayName("deliverToLocal: 연결되지 않은 사용자에게는 아무것도 하지 않는다")
    void deliverToLocal_notConnected_doesNothing() {
        // when - 예외 없이 실행되어야 함
        sseEmitterService.deliverToLocal(999L, createEvent());
    }

    @Test
    @DisplayName("sendToUsers: Redis 채널로 이벤트를 발행한다")
    void sendToUsers_publishesToRedis() {
        sseEmitterService.sendToUsers(List.of(1L, 2L), createEvent());

        then(stringRedisTemplate).should().convertAndSend(anyString(), anyString());
    }

    private SseNotificationEvent createEvent() {
        return new SseNotificationEvent(
                100L,
                NotificationTypeCode.BOOK_REPORT_CHECKED,
                "제목",
                "메시지",
                "/link",
                LocalDateTime.now()
        );
    }
}
