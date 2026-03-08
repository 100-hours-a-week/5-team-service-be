package com.example.doktoribackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitConfig {

    public static final String EXCHANGE    = "notification.exchange";
    public static final String QUEUE       = "notification.delivery.queue";
    public static final String DLQ         = "notification.delivery.dlq";
    public static final String ROUTING_KEY = "notification.delivery";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .ttl(600_000)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(ROUTING_KEY);
    }
}
