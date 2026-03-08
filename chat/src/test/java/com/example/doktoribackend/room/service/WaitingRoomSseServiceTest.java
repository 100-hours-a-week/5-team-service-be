package com.example.doktoribackend.room.service;

import com.example.doktoribackend.room.dto.WaitingRoomSseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WaitingRoomSseServiceTest {

    @Mock
    StringRedisTemplate stringRedisTemplate;

    WaitingRoomSseService waitingRoomSseService;

    @BeforeEach
    void setUp() {
        waitingRoomSseService = new WaitingRoomSseService(stringRedisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("subscribe: SSE emitter를 생성하고 반환한다")
    void subscribe_createsEmitter() {
        SseEmitter emitter = waitingRoomSseService.subscribe(1L);

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    @DisplayName("subscribe: 같은 방에 여러 유저가 구독할 수 있다")
    void subscribe_multipleUsersInSameRoom() {
        SseEmitter emitter1 = waitingRoomSseService.subscribe(1L);
        SseEmitter emitter2 = waitingRoomSseService.subscribe(1L);

        assertThat(emitter1).isNotSameAs(emitter2);
    }

    @Test
    @DisplayName("broadcast: 해당 방 Redis 채널에 waiting-room-update 이벤트를 발행한다")
    void broadcast_publishesToRedis() {
        waitingRoomSseService.broadcast(1L, null);

        then(stringRedisTemplate).should().convertAndSend(
                contains("waiting-room:sse:1"), contains("waiting-room-update"));
    }

    @Test
    @DisplayName("broadcastCancelledAndClose: 해당 방 Redis 채널에 room-cancelled 이벤트를 발행한다")
    void broadcastCancelledAndClose_publishesToRedis() {
        waitingRoomSseService.broadcastCancelledAndClose(1L);

        then(stringRedisTemplate).should().convertAndSend(
                contains("waiting-room:sse:1"), contains("room-cancelled"));
    }

    @Test
    @DisplayName("broadcastStartedAndClose: 해당 방 Redis 채널에 room-started 이벤트를 발행한다")
    void broadcastStartedAndClose_publishesToRedis() {
        waitingRoomSseService.broadcastStartedAndClose(1L, null);

        then(stringRedisTemplate).should().convertAndSend(
                contains("waiting-room:sse:1"), contains("room-started"));
    }

    @Test
    @DisplayName("deliverToLocal: 구독된 emitter에 이벤트를 전달한다")
    void deliverToLocal_connectedRoom_deliversEvent() {
        waitingRoomSseService.subscribe(1L);

        // when - 예외 없이 실행되어야 함
        waitingRoomSseService.deliverToLocal(1L, new WaitingRoomSseEvent("waiting-room-update", "{}"));
    }

    @Test
    @DisplayName("deliverToLocal: 구독자가 없는 방에는 아무것도 하지 않는다")
    void deliverToLocal_noSubscribers_doesNothing() {
        // when - 예외 없이 실행되어야 함
        waitingRoomSseService.deliverToLocal(999L, new WaitingRoomSseEvent("waiting-room-update", "{}"));
    }

    @Test
    @DisplayName("deliverToLocal: terminal 이벤트 수신 시 해당 방의 emitter를 모두 정리한다")
    void deliverToLocal_terminalEvent_clearsEmitters() {
        waitingRoomSseService.subscribe(1L);

        waitingRoomSseService.deliverToLocal(1L, new WaitingRoomSseEvent("room-cancelled", "방장이 나가 채팅방이 취소되었습니다."));

        // terminal 이벤트 후 같은 방에 다시 전달해도 아무 동작 없어야 함 (emitter 정리됨)
        waitingRoomSseService.deliverToLocal(1L, new WaitingRoomSseEvent("waiting-room-update", "{}"));
    }

    @Test
    @DisplayName("sendHeartbeat: 구독자가 없으면 아무것도 하지 않는다")
    void sendHeartbeat_noEmitters_doesNothing() {
        // when - 예외 없이 실행되어야 함
        waitingRoomSseService.sendHeartbeat();
    }

    @Test
    @DisplayName("broadcast: 서로 다른 방은 각자의 Redis 채널로 발행한다")
    void broadcast_differentRooms_publishToSeparateChannels() {
        waitingRoomSseService.broadcastCancelledAndClose(1L);
        waitingRoomSseService.broadcastCancelledAndClose(2L);

        then(stringRedisTemplate).should(times(1))
                .convertAndSend(contains("waiting-room:sse:1"), anyString());
        then(stringRedisTemplate).should(times(1))
                .convertAndSend(contains("waiting-room:sse:2"), anyString());
    }
}
