package com.example.doktoribackend.room.service;

import com.example.doktoribackend.message.service.ChatRoomRedisPublisher;
import com.example.doktoribackend.room.dto.ChatRoomStartResponse;
import com.example.doktoribackend.room.dto.NextRoundResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatRoomEventPublisher {

    private final WaitingRoomSseService waitingRoomSseService;
    private final ChatRoomRedisPublisher chatRoomRedisPublisher;

    public void broadcastWaitingRoomUpdate(Long roomId, WaitingRoomResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcast(roomId, response);
            }
        });
    }

    public void broadcastCancelled(Long roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcastCancelledAndClose(roomId);
            }
        });
    }

    public void broadcastStarted(Long roomId, ChatRoomStartResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                waitingRoomSseService.broadcastStartedAndClose(roomId, response);
            }
        });
    }

    public void broadcastNextRound(Long roomId, NextRoundResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatRoomRedisPublisher.publish(roomId, response);
            }
        });
    }

    public void broadcastEnded(Long roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatRoomRedisPublisher.publish(roomId, Map.of("type", "ROOM_ENDED"));
            }
        });
    }
}
