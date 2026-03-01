package com.example.doktoribackend.config;

import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class NotificationQueueConfig {

    private static final int QUEUE_CAPACITY = 10_000;

    @Bean
    public BlockingQueue<NotificationDeliveryTask> notificationDeliveryQueue() {
        return new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    }
}
