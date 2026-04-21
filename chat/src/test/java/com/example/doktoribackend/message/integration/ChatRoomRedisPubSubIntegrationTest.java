package com.example.doktoribackend.message.integration;

import com.example.doktoribackend.config.TestMongoConfig;
import com.example.doktoribackend.config.TestRedisConfig;
import com.example.doktoribackend.message.dto.ChatRoomBroadcastEvent;
import com.example.doktoribackend.message.service.ChatRoomRedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestMongoConfig.class, TestRedisConfig.class})
class ChatRoomRedisPubSubIntegrationTest {

    @Autowired
    private ChatRoomRedisPublisher chatRoomRedisPublisher;

    @Autowired
    private RedisMessageListenerContainer chatRoomRedisListenerContainer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Redis Pub/Sub: publish된 메시지가 subscriber에게 전달된다")
    void publishAndSubscribe() throws Exception {
        // given
        Long roomId = 999L;
        Map<String, Object> payload = Map.of(
                "messageId", "test-msg-001",
                "senderId", 1L,
                "textMessage", "Redis Pub/Sub 테스트"
        );

        BlockingQueue<ChatRoomBroadcastEvent> receivedEvents = new LinkedBlockingQueue<>();

        // 별도 리스너로 수신 확인 (기존 ChatRoomRedisSubscriber와 독립적으로 검증)
        chatRoomRedisListenerContainer.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
                try {
                    ChatRoomBroadcastEvent event = objectMapper.readValue(
                            message.getBody(), ChatRoomBroadcastEvent.class);
                    receivedEvents.offer(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, new PatternTopic("chat-room:broadcast:" + roomId));

        Thread.sleep(500); // 구독 등록 대기

        // when
        chatRoomRedisPublisher.publish(roomId, payload);

        // then
        ChatRoomBroadcastEvent received = receivedEvents.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.roomId()).isEqualTo(roomId);

        Map<String, Object> receivedPayload = objectMapper.readValue(received.payload(), Map.class);
        assertThat(receivedPayload).containsEntry("textMessage", "Redis Pub/Sub 테스트");
        assertThat(receivedPayload).containsEntry("messageId", "test-msg-001");
    }

    @Test
    @DisplayName("Redis Pub/Sub: 다른 roomId의 메시지는 수신하지 않는다")
    void publishToOtherRoom_notReceived() throws Exception {
        // given
        Long targetRoomId = 888L;
        Long otherRoomId = 777L;

        BlockingQueue<ChatRoomBroadcastEvent> receivedEvents = new LinkedBlockingQueue<>();

        chatRoomRedisListenerContainer.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
                try {
                    ChatRoomBroadcastEvent event = objectMapper.readValue(
                            message.getBody(), ChatRoomBroadcastEvent.class);
                    receivedEvents.offer(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, new PatternTopic("chat-room:broadcast:" + targetRoomId));

        Thread.sleep(500);

        // when: 다른 roomId로 발행
        chatRoomRedisPublisher.publish(otherRoomId, Map.of("text", "다른 방 메시지"));

        // then: targetRoomId 리스너는 수신하지 않음
        ChatRoomBroadcastEvent received = receivedEvents.poll(2, TimeUnit.SECONDS);
        assertThat(received).isNull();
    }
}
