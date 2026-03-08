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
import static org.mockito.BDDMockito.given;
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
    @DisplayName("SSE 미연결 유저는 SSE + FCM 모두 발송 후 ACK한다")
    void consume_sseDisconnected_sendsBothAndAcks() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of(1L));

        consumer.consume(task, channel, 1L);

        then(sseEmitterService).should().sendToUsers(List.of(1L), task.sseEvent());
        then(fcmService).should().sendToUsers(List.of(1L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(1L, false, false);
    }

    @Test
    @DisplayName("SSE 연결 중인 유저는 SSE만 발송하고 FCM은 발송하지 않는다")
    void consume_sseConnected_sendsOnlySse() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of());

        consumer.consume(task, channel, 1L);

        then(sseEmitterService).should().sendToUsers(List.of(1L), task.sseEvent());
        then(fcmService).should(never()).sendToUsers(anyList(), anyString(), anyString(), anyString());
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("일부는 SSE 연결, 일부는 미연결이면 미연결 유저에게만 FCM 발송한다")
    void consume_mixed_sendsFcmOnlyToDisconnected() throws IOException {
        List<Long> userIds = List.of(1L, 2L, 3L);
        NotificationDeliveryTask task = createTask(userIds);
        given(sseEmitterService.filterSseDisconnectedUsers(userIds)).willReturn(List.of(2L, 3L));

        consumer.consume(task, channel, 1L);

        then(sseEmitterService).should().sendToUsers(userIds, task.sseEvent());
        then(fcmService).should().sendToUsers(List.of(2L, 3L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("FCM 실패 시 NACK하여 DLQ로 라우팅한다")
    void consume_fcmFails_nacks() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of(1L));
        willThrow(new RuntimeException("FCM 에러"))
                .given(fcmService).sendToUsers(anyList(), anyString(), anyString(), anyString());

        consumer.consume(task, channel, 1L);

        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(1L, false);
    }

    @Test
    @DisplayName("SSE 실패 시에도 FCM 미연결 유저에게 발송 후 ACK한다")
    void consume_sseFails_fcmDeliveredAndAcks() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of(1L));
        willThrow(new RuntimeException("SSE 에러"))
                .given(sseEmitterService).sendToUsers(anyList(), any(SseNotificationEvent.class));

        consumer.consume(task, channel, 1L);

        then(fcmService).should().sendToUsers(List.of(1L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("DLQ 메시지는 로그 후 ACK하며 SSE, FCM을 발송하지 않는다")
    void handleDeadLetter_acks() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));

        consumer.handleDeadLetter(task, channel, 1L);

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
