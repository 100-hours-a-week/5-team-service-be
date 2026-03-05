package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.config.NotificationRabbitConfig;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class RabbitMQNotificationQueue implements NotificationEnqueuePort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void enqueue(NotificationDeliveryTask task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(
                        NotificationRabbitConfig.EXCHANGE,
                        NotificationRabbitConfig.ROUTING_KEY,
                        task
                );
            }
        });
    }
}
