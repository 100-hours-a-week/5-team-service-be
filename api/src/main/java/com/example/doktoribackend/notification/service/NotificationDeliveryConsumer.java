package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryConsumer {

    private final BlockingQueue<NotificationDeliveryTask> notificationDeliveryQueue;
    private final SseEmitterService sseEmitterService;
    private final FcmService fcmService;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    void start() {
        consumerThread = Thread.ofVirtual()
                .name("notification-delivery-consumer")
                .start(this::consumeLoop);
    }

    private void consumeLoop() {
        while (running) {
            try {
                NotificationDeliveryTask task = notificationDeliveryQueue.take();
                deliver(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to deliver notification", e);
            }
        }
    }

    private void deliver(NotificationDeliveryTask task) {
        List<Long> userIds = task.userIds();

        try {
            sseEmitterService.sendToUsers(userIds, task.sseEvent());
        } catch (Exception e) {
            log.error("SSE delivery failed for userIds: {}", userIds, e);
        }

        try {
            fcmService.sendToUsers(userIds, task.title(), task.message(), task.linkPath());
        } catch (Exception e) {
            log.error("FCM delivery failed for userIds: {}", userIds, e);
        }
    }

    @PreDestroy
    void shutdown() {
        running = false;
        consumerThread.interrupt();

        List<NotificationDeliveryTask> remaining = new ArrayList<>();
        notificationDeliveryQueue.drainTo(remaining);

        log.info("Draining {} remaining notification tasks", remaining.size());

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        for (NotificationDeliveryTask task : remaining) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("Shutdown timeout reached, {} tasks not delivered",
                        remaining.size() - remaining.indexOf(task));
                break;
            }
            try {
                deliver(task);
            } catch (Exception e) {
                log.error("Failed to deliver task during shutdown", e);
            }
        }
    }
}
