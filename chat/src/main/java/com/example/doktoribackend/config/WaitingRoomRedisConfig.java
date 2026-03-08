package com.example.doktoribackend.config;

import com.example.doktoribackend.room.service.WaitingRoomRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class WaitingRoomRedisConfig {

    static final String WAITING_ROOM_CHANNEL_PATTERN = "waiting-room:sse:*";

    @Bean
    public RedisMessageListenerContainer waitingRoomRedisListenerContainer(
            RedisConnectionFactory factory,
            WaitingRoomRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber, new PatternTopic(WAITING_ROOM_CHANNEL_PATTERN));
        return container;
    }
}
