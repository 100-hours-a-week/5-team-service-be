package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryConsumerTest {

    @Mock
    SseEmitterService sseEmitterService;

    @Mock
    FcmService fcmService;

    BlockingQueue<NotificationDeliveryTask> queue;
    NotificationDeliveryConsumer consumer;

    @BeforeEach
    void setUp() {
        queue = new LinkedBlockingQueue<>(100);
        consumer = new NotificationDeliveryConsumer(queue, sseEmitterService, fcmService);
        consumer.start();
    }

    @AfterEach
    void tearDown() {
        consumer.shutdown();
    }

    @Test
    @DisplayName("큐에 태스크를 넣으면 SSE와 FCM으로 발송한다")
    void consume_deliversBothSseAndFcm() {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));

        // when
        queue.offer(task);

        // then
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(List.of(1L), task.sseEvent());
        then(fcmService).should(timeout(3000))
                .sendToUsers(List.of(1L), "제목", "메시지", "/link");
    }

    @Test
    @DisplayName("여러 사용자에게 일괄 발송한다")
    void consume_batchDelivery() {
        // given
        List<Long> userIds = List.of(1L, 2L, 3L);
        NotificationDeliveryTask task = createTask(userIds);

        // when
        queue.offer(task);

        // then
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(userIds, task.sseEvent());
        then(fcmService).should(timeout(3000))
                .sendToUsers(userIds, "제목", "메시지", "/link");
    }

    @Test
    @DisplayName("SSE 발송이 실패해도 FCM 발송은 수행한다")
    void consume_sseFails_fcmStillDelivered() {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));
        willThrow(new RuntimeException("SSE 에러"))
                .given(sseEmitterService).sendToUsers(anyList(), any(SseNotificationEvent.class));

        // when
        queue.offer(task);

        // then
        then(fcmService).should(timeout(3000))
                .sendToUsers(List.of(1L), "제목", "메시지", "/link");
    }

    @Test
    @DisplayName("FCM 발송이 실패해도 예외를 던지지 않는다")
    void consume_fcmFails_doesNotThrow() {
        // given
        NotificationDeliveryTask task = createTask(List.of(1L));
        willThrow(new RuntimeException("FCM 에러"))
                .given(fcmService).sendToUsers(anyList(), anyString(), anyString(), anyString());

        // when
        queue.offer(task);

        // then - SSE는 정상 호출되어야 함
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(List.of(1L), task.sseEvent());
    }

    @Test
    @DisplayName("여러 태스크를 순서대로 처리한다")
    void consume_multipleTasksProcessedInOrder() {
        // given
        NotificationDeliveryTask task1 = createTask(List.of(1L));
        NotificationDeliveryTask task2 = createTask(List.of(2L));

        // when
        queue.offer(task1);
        queue.offer(task2);

        // then
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(List.of(1L), task1.sseEvent());
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(List.of(2L), task2.sseEvent());
        then(fcmService).should(timeout(3000))
                .sendToUsers(List.of(1L), "제목", "메시지", "/link");
        then(fcmService).should(timeout(3000))
                .sendToUsers(List.of(2L), "제목", "메시지", "/link");
    }

    @Test
    @DisplayName("shutdown 시 큐에 남은 태스크를 drain하여 처리한다")
    void shutdown_drainsRemainingTasks() throws InterruptedException {
        // given - consumer를 먼저 종료하고 큐에 태스크를 넣어 drain 테스트
        consumer.shutdown();

        // 새 큐 & 컨슈머로 다시 테스트
        queue = new LinkedBlockingQueue<>(100);
        consumer = new NotificationDeliveryConsumer(queue, sseEmitterService, fcmService);
        // start()를 호출하지 않아서 큐에서 take()하는 스레드가 없음

        NotificationDeliveryTask task = createTask(List.of(99L));
        queue.offer(task);

        assertThat(queue).hasSize(1);

        // when - shutdown이 drain하여 남은 태스크를 처리
        consumer.start();
        // 잠깐 대기 후 shutdown으로 drain
        TimeUnit.MILLISECONDS.sleep(100);
        consumer.shutdown();

        // then - 큐가 비워져야 함
        assertThat(queue).isEmpty();
    }

    @Test
    @DisplayName("큐가 비어있으면 태스크가 들어올 때까지 대기한다")
    void consume_emptyQueue_waitsForTask() throws InterruptedException {
        // given - 큐가 비어있으므로 아무것도 호출되지 않아야 함
        TimeUnit.MILLISECONDS.sleep(200);

        then(sseEmitterService).should(never()).sendToUsers(anyList(), any(SseNotificationEvent.class));
        then(fcmService).should(never()).sendToUsers(anyList(), anyString(), anyString(), anyString());

        // when - 태스크를 넣으면 즉시 처리
        NotificationDeliveryTask task = createTask(List.of(1L));
        queue.offer(task);

        // then
        then(sseEmitterService).should(timeout(3000))
                .sendToUsers(List.of(1L), task.sseEvent());
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
