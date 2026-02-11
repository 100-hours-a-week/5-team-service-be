package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    SseEmitterService sseEmitterService;

    @BeforeEach
    void setUp() {
        sseEmitterService = new SseEmitterService();
    }

    @Test
    @DisplayName("subscribe: SSE 연결을 생성하고 반환한다")
    void subscribe_createsEmitter() {
        // given
        Long userId = 1L;

        // when
        SseEmitter emitter = sseEmitterService.subscribe(userId);

        // then
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);
    }

    @Test
    @DisplayName("subscribe: 기존 연결이 있으면 완료하고 새 연결을 생성한다")
    void subscribe_existingConnection_replacesIt() {
        // given
        Long userId = 1L;
        SseEmitter firstEmitter = sseEmitterService.subscribe(userId);

        // when
        SseEmitter secondEmitter = sseEmitterService.subscribe(userId);

        // then
        assertThat(secondEmitter).isNotNull();
        assertThat(secondEmitter).isNotSameAs(firstEmitter);
    }

    @Test
    @DisplayName("sendToUser: 연결된 사용자에게 이벤트를 전송한다")
    void sendToUser_connectedUser_sendsEvent() {
        // given
        Long userId = 1L;
        sseEmitterService.subscribe(userId);

        SseNotificationEvent event = new SseNotificationEvent(
                100L,
                NotificationTypeCode.BOOK_REPORT_CHECKED,
                "제목",
                "메시지",
                "/link",
                LocalDateTime.now()
        );

        // when - 예외 없이 실행되어야 함
        sseEmitterService.sendToUser(userId, event);

        // then - 예외가 발생하지 않으면 성공
    }

    @Test
    @DisplayName("sendToUser: 연결되지 않은 사용자에게는 아무것도 하지 않는다")
    void sendToUser_notConnected_doesNothing() {
        // given
        Long userId = 999L;
        SseNotificationEvent event = new SseNotificationEvent(
                100L,
                NotificationTypeCode.BOOK_REPORT_CHECKED,
                "제목",
                "메시지",
                "/link",
                LocalDateTime.now()
        );

        // when - 예외 없이 실행되어야 함
        sseEmitterService.sendToUser(userId, event);

        // then - 예외가 발생하지 않으면 성공
    }

    @Test
    @DisplayName("sendToUsers: 여러 사용자에게 이벤트를 전송한다")
    void sendToUsers_multipleUsers_sendsToAll() {
        // given
        sseEmitterService.subscribe(1L);
        sseEmitterService.subscribe(2L);

        SseNotificationEvent event = new SseNotificationEvent(
                100L,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                "제목",
                "메시지",
                "/link",
                LocalDateTime.now()
        );

        // when - 예외 없이 실행되어야 함
        sseEmitterService.sendToUsers(List.of(1L, 2L, 3L), event);
    }
}
