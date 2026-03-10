package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.config.NotificationRabbitConfig;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class NotificationDeliveryConsumer {

    private final SseEmitterService sseEmitterService;
    private final FcmService fcmService;
    private final Counter deliverySuccessCounter;
    private final Counter deliveryFailureCounter;

    public NotificationDeliveryConsumer(SseEmitterService sseEmitterService,
                                       FcmService fcmService,
                                       MeterRegistry meterRegistry) {
        this.sseEmitterService = sseEmitterService;
        this.fcmService = fcmService;
        this.deliverySuccessCounter = Counter.builder("notification.delivery")
                .tag("result", "success")
                .description("Notification delivery attempts")
                .register(meterRegistry);
        this.deliveryFailureCounter = Counter.builder("notification.delivery")
                .tag("result", "failure")
                .description("Notification delivery attempts")
                .register(meterRegistry);
    }

    @RabbitListener(queues = NotificationRabbitConfig.QUEUE)
    public void consume(
            NotificationDeliveryTask task,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            deliver(task);
            channel.basicAck(deliveryTag, false);
            deliverySuccessCounter.increment();
        } catch (Exception e) {
            log.error("Notification delivery failed for userIds: {}, routing to DLQ", task.userIds(), e);
            channel.basicNack(deliveryTag, false, false);
            deliveryFailureCounter.increment();
        }
    }

    @RabbitListener(queues = NotificationRabbitConfig.DLQ)
    public void handleDeadLetter(
            NotificationDeliveryTask task,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.error("Notification permanently failed. userIds={}, title={}", task.userIds(), task.title());
        channel.basicAck(deliveryTag, false);
    }

    private void deliver(NotificationDeliveryTask task) {
        List<Long> userIds = task.userIds();
        List<Long> fcmTargetIds = sseEmitterService.filterSseDisconnectedUsers(userIds);

        try {
            sseEmitterService.sendToUsers(userIds, task.sseEvent());
        } catch (Exception e) {
            log.error("SSE delivery failed for userIds: {}", userIds, e);
        }

        if (!fcmTargetIds.isEmpty()) {
            fcmService.sendToUsers(fcmTargetIds, task.title(), task.message(), task.linkPath());
        }
    }
}
