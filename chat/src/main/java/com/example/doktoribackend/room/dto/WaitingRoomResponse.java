package com.example.doktoribackend.room.dto;

import java.util.List;

public record WaitingRoomResponse(
        Long roomId,
        int agreeCount,
        int disagreeCount,
        int maxPerPosition,
        List<WaitingRoomMemberItem> members
) {}
