package com.example.doktoribackend.room.service;

import com.example.doktoribackend.room.dto.WaitingRoomSseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingRoomRedisSubscriber implements MessageListener {

    private static final String CHANNEL_PREFIX = "waiting-room:sse:";

    private final WaitingRoomSseService waitingRoomSseService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            Long roomId = Long.parseLong(channel.substring(CHANNEL_PREFIX.length()));
            WaitingRoomSseEvent event = objectMapper.readValue(message.getBody(), WaitingRoomSseEvent.class);
            waitingRoomSseService.deliverToLocal(roomId, event);
        } catch (Exception e) {
            log.error("Failed to process Redis waiting room SSE message", e);
        }
    }
}
