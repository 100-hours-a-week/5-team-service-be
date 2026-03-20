package com.example.doktoribackend.config;

import com.example.doktoribackend.message.service.ChatRoomRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ChatRoomRedisConfig {

    static final String CHAT_ROOM_CHANNEL_PATTERN = "chat-room:broadcast:*";

    @Bean
    public RedisMessageListenerContainer chatRoomRedisListenerContainer(
            RedisConnectionFactory factory,
            ChatRoomRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber, new PatternTopic(CHAT_ROOM_CHANNEL_PATTERN));
        return container;
    }
}
