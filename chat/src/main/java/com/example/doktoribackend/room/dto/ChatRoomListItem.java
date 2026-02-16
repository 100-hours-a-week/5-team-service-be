package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.room.domain.ChattingRoom;

public record ChatRoomListItem(
        Long roomId,
        String topic,
        String description,
        Integer capacity,
        Integer currentMemberCount
) {

    public static ChatRoomListItem from(ChattingRoom room) {
        return new ChatRoomListItem(
                room.getId(),
                room.getTopic(),
                room.getDescription(),
                room.getCapacity(),
                room.getCurrentMemberCount()
        );
    }
}
