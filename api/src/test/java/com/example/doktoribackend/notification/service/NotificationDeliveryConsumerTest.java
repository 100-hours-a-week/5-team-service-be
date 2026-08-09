package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.config.NotificationRabbitConfig;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.FcmSendResult;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    RabbitTemplate rabbitTemplate;

    @Mock
    Channel channel;

    NotificationDeliveryConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationDeliveryConsumer(
                sseEmitterService, fcmService, rabbitTemplate, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("SSE 미연결 유저는 SSE + FCM 모두 발송 후 ACK한다")
    void consume_sseDisconnected_sendsBothAndAcks() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of(1L));
        givenFcmResult(FcmSendResult.of(1, List.of(), List.of()));

        consumer.consume(task, channel, message(0), 1L);

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

        consumer.consume(task, channel, message(0), 1L);

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
        givenFcmResult(FcmSendResult.of(2, List.of(), List.of()));

        consumer.consume(task, channel, message(0), 1L);

        then(sseEmitterService).should().sendToUsers(userIds, task.sseEvent());
        then(fcmService).should().sendToUsers(List.of(2L, 3L), "제목", "메시지", "/link");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("전원 전달에 성공하면 재발행하지 않는다")
    void consume_allDelivered_doesNotRepublish() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L, 2L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L, 2L))).willReturn(List.of(1L, 2L));
        givenFcmResult(FcmSendResult.of(2, List.of(), List.of()));

        consumer.consume(task, channel, message(0), 1L);

        verify(channel).basicAck(1L, false);
        verify(rabbitTemplate, never())
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("FCM 일시 실패는 실패한 유저에게만 재발행한다 (성공한 유저 중복 발송 방지)")
    void consume_partialTransientFailure_republishesOnlyFailedUsers() throws IOException {
        List<Long> userIds = List.of(1L, 2L, 3L);
        NotificationDeliveryTask task = createTask(userIds);
        given(sseEmitterService.filterSseDisconnectedUsers(userIds)).willReturn(userIds);
        givenFcmResult(FcmSendResult.of(2, List.of(), List.of(3L)));

        consumer.consume(task, channel, message(0), 1L);

        NotificationDeliveryTask republished = captureRepublished(NotificationRabbitConfig.WAIT_QUEUE);
        assertThat(republished.userIds()).containsExactly(3L);
        assertThat(republished.title()).isEqualTo("제목");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("FCM 배치 전체 실패는 전원을 재발행한다")
    void consume_fcmTotalFailure_republishesAllTargets() throws IOException {
        List<Long> userIds = List.of(1L, 2L);
        NotificationDeliveryTask task = createTask(userIds);
        given(sseEmitterService.filterSseDisconnectedUsers(userIds)).willReturn(userIds);
        givenFcmResult(FcmSendResult.totalFailure(userIds));

        consumer.consume(task, channel, message(0), 1L);

        NotificationDeliveryTask republished = captureRepublished(NotificationRabbitConfig.WAIT_QUEUE);
        assertThat(republished.userIds()).containsExactlyInAnyOrderElementsOf(userIds);
    }

    @Test
    @DisplayName("SSE만 실패하면 SSE 대상 유저만 재발행한다")
    void consume_sseFailsOnly_republishesSseTargets() throws IOException {
        List<Long> userIds = List.of(1L, 2L, 3L);
        NotificationDeliveryTask task = createTask(userIds);
        given(sseEmitterService.filterSseDisconnectedUsers(userIds)).willReturn(List.of(3L));
        givenFcmResult(FcmSendResult.of(1, List.of(), List.of()));
        willThrow(new RuntimeException("SSE 에러"))
                .given(sseEmitterService).sendToUsers(anyList(), any(SseNotificationEvent.class));

        consumer.consume(task, channel, message(0), 1L);

        NotificationDeliveryTask republished = captureRepublished(NotificationRabbitConfig.WAIT_QUEUE);
        assertThat(republished.userIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("무효 토큰만 실패한 경우는 재발행하지 않는다")
    void consume_onlyDiscardedTokens_doesNotRepublish() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L, 2L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L, 2L))).willReturn(List.of(1L, 2L));
        givenFcmResult(FcmSendResult.of(1, List.of("dead-token"), List.of()));

        consumer.consume(task, channel, message(0), 1L);

        verify(rabbitTemplate, never())
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 DLQ로 보낸다")
    void consume_maxRetryReached_sendsToDlq() throws IOException {
        NotificationDeliveryTask task = createTask(List.of(1L));
        given(sseEmitterService.filterSseDisconnectedUsers(List.of(1L))).willReturn(List.of(1L));
        givenFcmResult(FcmSendResult.totalFailure(List.of(1L)));

        consumer.consume(task, channel, message(NotificationRabbitConfig.MAX_RETRY_COUNT), 1L);

        NotificationDeliveryTask dead = captureRepublished(NotificationRabbitConfig.DLQ);
        assertThat(dead.userIds()).containsExactly(1L);
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

    private void givenFcmResult(FcmSendResult result) {
        given(fcmService.sendToUsers(anyList(), anyString(), anyString(), anyString())).willReturn(result);
    }

    private NotificationDeliveryTask captureRepublished(String queue) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(""), eq(queue), captor.capture(),
                any(MessagePostProcessor.class));
        return (NotificationDeliveryTask) captor.getValue();
    }

    private Message message(int retryCount) {
        MessageProperties properties = new MessageProperties();
        if (retryCount > 0) {
            properties.getHeaders().put("x-retry-count", retryCount);
        }
        return new Message(new byte[0], properties);
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
