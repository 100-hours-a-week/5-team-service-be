package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SseEmitterServiceTest {

    @Mock
    StringRedisTemplate stringRedisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        sseEmitterService = new SseEmitterService(stringRedisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    @DisplayName("subscribe: SSE 연결을 생성하고 Redis에 연결 상태를 기록한다")
    void subscribe_createsEmitterAndSetsRedisKey() {
        sseEmitterService.subscribe(1L);

        then(valueOperations).should().set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("subscribe: 기존 연결이 있으면 완료하고 새 연결을 생성한다")
    void subscribe_existingConnection_replacesIt() {
        sseEmitterService.subscribe(1L);
        sseEmitterService.subscribe(1L);

        // Redis key는 두 번 set 된다 (첫 구독 + 재구독)
        then(valueOperations).should(times(2)).set(anyString(), anyString(), any());
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
    @DisplayName("sendToUsers: SSE 연결된 유저에게만 Redis 채널로 이벤트를 발행한다")
    void sendToUsers_publishesOnlyToConnectedUsers() {
        given(stringRedisTemplate.hasKey("notification:sse:connected:1")).willReturn(true);
        given(stringRedisTemplate.hasKey("notification:sse:connected:2")).willReturn(false);

        sseEmitterService.sendToUsers(List.of(1L, 2L), createEvent());

        then(stringRedisTemplate).should(times(1)).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("sendToUsers: 연결된 유저가 없으면 Redis에 발행하지 않는다")
    void sendToUsers_noConnectedUsers_doesNotPublish() {
        given(stringRedisTemplate.hasKey(anyString())).willReturn(false);

        sseEmitterService.sendToUsers(List.of(1L, 2L), createEvent());

        then(stringRedisTemplate).should(never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("filterSseDisconnectedUsers: SSE 미연결 유저만 반환한다")
    void filterSseDisconnectedUsers_returnsOnlyDisconnected() {
        given(stringRedisTemplate.hasKey("notification:sse:connected:1")).willReturn(true);
        given(stringRedisTemplate.hasKey("notification:sse:connected:2")).willReturn(false);

        List<Long> result = sseEmitterService.filterSseDisconnectedUsers(List.of(1L, 2L));

        assertThat(result).containsExactly(2L);
    }

    @Test
    @DisplayName("filterSseDisconnectedUsers: 전체 연결된 경우 빈 리스트를 반환한다")
    void filterSseDisconnectedUsers_allConnected_returnsEmpty() {
        given(stringRedisTemplate.hasKey(anyString())).willReturn(true);

        List<Long> result = sseEmitterService.filterSseDisconnectedUsers(List.of(1L, 2L));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("sendHeartbeat: 구독자가 없으면 아무것도 하지 않는다")
    void sendHeartbeat_noEmitters_doesNothing() {
        sseEmitterService.sendHeartbeat();

        then(stringRedisTemplate).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("sendHeartbeat: 활성 emitter가 있어도 연결이 살아있으면 정리하지 않는다")
    void sendHeartbeat_withActiveEmitter_doesNotCleanUp() {
        sseEmitterService.subscribe(1L);

        sseEmitterService.sendHeartbeat();

        then(stringRedisTemplate).should(never()).delete(anyString());
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
