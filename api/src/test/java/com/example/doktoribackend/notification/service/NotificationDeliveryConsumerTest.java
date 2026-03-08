package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryConsumerTest {

    @Mock
    SseEmitterService sseEmitterService;

    @Mock
    FcmService fcmService;

    @Mock
    Channel channel;

    @InjectMocks
    NotificationDeliveryConsumer consumer;

    @Test
    @DisplayName("정상 처리 시 SSE, FCM 발송 후 ACK한다")
    void consume_success_acks() throws IOException {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));

        // when
        consumer.consume(task, channel, 1L);

        // then
        then(sseEmitterService).should().sendToUsers(List.of(1L), task.sseEvent());
        then(fcmService).should().sendToUsers(List.of(1L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(1L, false, false);
    }

    @Test
    @DisplayName("FCM 실패 시 NACK하여 DLQ로 라우팅한다")
    void consume_fcmFails_nacks() throws IOException {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));
        willThrow(new RuntimeException("FCM 에러"))
                .given(fcmService).sendToUsers(anyList(), anyString(), anyString(), anyString());

        // when
        consumer.consume(task, channel, 1L);

        // then
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(1L, false);
    }

    @Test
    @DisplayName("SSE 실패 시에도 FCM 발송 후 ACK한다")
    void consume_sseFails_fcmDeliveredAndAcks() throws IOException {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));
        willThrow(new RuntimeException("SSE 에러"))
                .given(sseEmitterService).sendToUsers(anyList(), any(SseNotificationEvent.class));

        // when
        consumer.consume(task, channel, 1L);

        // then
        then(fcmService).should().sendToUsers(List.of(1L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("여러 사용자에게 일괄 발송 후 ACK한다")
    void consume_batchDelivery_acks() throws IOException {
        // given
        List<Long> userIds = List.of(1L, 2L, 3L);
        NotificationDeliveryTask task = createTask(userIds);

        // when
        consumer.consume(task, channel, 1L);

        // then
        then(sseEmitterService).should().sendToUsers(userIds, task.sseEvent());
        then(fcmService).should().sendToUsers(userIds, "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("DLQ 메시지는 로그 후 ACK한다")
    void handleDeadLetter_acks() throws IOException {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));

        // when
        consumer.handleDeadLetter(task, channel, 1L);

        // then
        verify(channel).basicAck(1L, false);
        then(sseEmitterService).should(never()).sendToUsers(anyList(), any());
        then(fcmService).should(never()).sendToUsers(anyList(), anyString(), anyString(), anyString());
    }

    private NotificationDeliveryTask createTask(List<Long> userIds) {
        SseNotificationEvent sseEvent = new SseNotificationEvent(
                null,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                "제목",
                "메시지",
                "/link",
                LocalDateTime.now()
        );
        return new NotificationDeliveryTask(userIds, "제목", "메시지", "/link", sseEvent);
    }
}
