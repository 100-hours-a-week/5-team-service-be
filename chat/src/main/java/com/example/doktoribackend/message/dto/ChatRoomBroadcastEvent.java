package com.example.doktoribackend.message.dto;

/**
 * Redis Pub/Sub 채널을 통해 전파되는 채팅방 브로드캐스트 이벤트.
 *
 * @param roomId  대상 채팅방 ID
 * @param payload JSON 직렬화된 브로드캐스트 데이터
 */
public record ChatRoomBroadcastEvent(
        Long roomId,
        String payload
) {}
