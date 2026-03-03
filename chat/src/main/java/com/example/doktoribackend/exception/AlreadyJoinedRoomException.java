package com.example.doktoribackend.exception;

import com.example.doktoribackend.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class AlreadyJoinedRoomException extends CustomException {

    private final Long roomId;

    public AlreadyJoinedRoomException(Long roomId) {
        super(ErrorCode.CHAT_ROOM_ALREADY_JOINED);
        this.roomId = roomId;
    }
}
