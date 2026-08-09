package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.config.NotificationRabbitConfig;
import com.example.doktoribackend.notification.dto.FcmSendResult;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationDeliveryConsumer {

    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final SseEmitterService sseEmitterService;
    private final FcmService fcmService;
    private final RabbitTemplate rabbitTemplate;
    private final Counter deliverySuccessCounter;
    private final Counter deliveryFailureCounter;
    private final Counter deliveryRetriedCounter;
    private final Counter deliveryPermanentFailureCounter;

    public NotificationDeliveryConsumer(SseEmitterService sseEmitterService,
                                       FcmService fcmService,
                                       RabbitTemplate rabbitTemplate,
                                       MeterRegistry meterRegistry) {
        this.sseEmitterService = sseEmitterService;
        this.fcmService = fcmService;
        this.rabbitTemplate = rabbitTemplate;
        this.deliverySuccessCounter = Counter.builder("notification.delivery")
                .tag("result", "success")
                .description("Notification delivery attempts")
                .register(meterRegistry);
        this.deliveryFailureCounter = Counter.builder("notification.delivery")
                .tag("result", "failure")
                .description("Notification delivery attempts")
                .register(meterRegistry);
        this.deliveryRetriedCounter = Counter.builder("notification.delivery")
                .tag("result", "retried")
                .description("Notification delivery retried")
                .register(meterRegistry);
        this.deliveryPermanentFailureCounter = Counter.builder("notification.delivery")
                .tag("result", "permanent_failure")
                .description("Notification delivery permanently failed")
                .register(meterRegistry);
    }

    /**
     * ack 를 먼저 하고 실패분만 수동으로 재발행한다. nack 재큐잉은 즉시 되돌아와 무한 루프가 되므로
     * 지연 재시도는 wait 큐의 TTL 로 처리한다.
     */
    @RabbitListener(queues = NotificationRabbitConfig.QUEUE)
    public void consume(
            NotificationDeliveryTask task,
            Channel channel,
            Message message,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        List<Long> retryUserIds;
        try {
            retryUserIds = deliver(task);
        } catch (Exception e) {
            log.error("Notification delivery aborted unexpectedly for userIds: {}", task.userIds(), e);
            retryUserIds = task.userIds();
        }

        channel.basicAck(deliveryTag, false);

        if (retryUserIds.isEmpty()) {
            deliverySuccessCounter.increment();
            return;
        }

        int retryCount = getRetryCount(message);
        NotificationDeliveryTask retryTask = task.withUserIds(retryUserIds);

        if (retryCount < NotificationRabbitConfig.MAX_RETRY_COUNT) {
            log.warn("Notification delivery failed (attempt {}/{}), retrying for userIds: {}",
                    retryCount + 1, NotificationRabbitConfig.MAX_RETRY_COUNT, retryUserIds);
            sendToWaitQueue(retryTask, retryCount + 1);
            deliveryRetriedCounter.increment();
        } else {
            log.error("Notification delivery permanently failed after {} retries for userIds: {}",
                    NotificationRabbitConfig.MAX_RETRY_COUNT, retryUserIds);
            sendToDlq(retryTask, retryCount);
            deliveryFailureCounter.increment();
        }
    }

    @RabbitListener(queues = NotificationRabbitConfig.DLQ)
    public void handleDeadLetter(
            NotificationDeliveryTask task,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.error("Notification permanently failed after max retries. userIds={}, title={}",
                task.userIds(), task.title());
        deliveryPermanentFailureCounter.increment();
        channel.basicAck(deliveryTag, false);
    }

    /**
     * @return 재전송이 필요한 사용자. 비어 있으면 전원 전달에 성공한 것이다.
     */
    private List<Long> deliver(NotificationDeliveryTask task) {
        List<Long> userIds = task.userIds();
        // 매 시도마다 다시 계산한다. 재시도 시점에는 접속 상태가 바뀌어 있을 수 있다.
        List<Long> fcmTargetIds = sseEmitterService.filterSseDisconnectedUsers(userIds);
        List<Long> sseTargetIds = userIds.stream()
                .filter(id -> !fcmTargetIds.contains(id))
                .toList();

        List<Long> retryUserIds = new ArrayList<>();

        try {
            sseEmitterService.sendToUsers(userIds, task.sseEvent());
        } catch (Exception e) {
            log.error("SSE delivery failed for userIds: {}", sseTargetIds, e);
            // SSE 는 Redis pub/sub 이라 접속자 전체가 함께 실패한다.
            retryUserIds.addAll(sseTargetIds);
        }

        if (!fcmTargetIds.isEmpty()) {
            FcmSendResult result = fcmService.sendToUsers(
                    fcmTargetIds, task.title(), task.message(), task.linkPath());
            retryUserIds.addAll(result.retryableUserIds());
        }

        return List.copyOf(new LinkedHashSet<>(retryUserIds));
    }

    private int getRetryCount(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object count = headers.get(RETRY_COUNT_HEADER);
        if (count instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private void sendToWaitQueue(NotificationDeliveryTask task, int retryCount) {
        long delay = NotificationRabbitConfig.getRetryDelay(retryCount);
        rabbitTemplate.convertAndSend("", NotificationRabbitConfig.WAIT_QUEUE, task, msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(delay));
            msg.getMessageProperties().getHeaders().put(RETRY_COUNT_HEADER, retryCount);
            return msg;
        });
    }

    private void sendToDlq(NotificationDeliveryTask task, int retryCount) {
        rabbitTemplate.convertAndSend("", NotificationRabbitConfig.DLQ, task, msg -> {
            msg.getMessageProperties().getHeaders().put(RETRY_COUNT_HEADER, retryCount);
            return msg;
        });
    }
}
