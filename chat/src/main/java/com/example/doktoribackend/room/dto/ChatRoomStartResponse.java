package com.example.doktoribackend.room.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomStartResponse(
        String topic,
        List<ChatStartMemberItem> agreeMembers,
        List<ChatStartMemberItem> disagreeMembers,
        int currentRound,
        LocalDateTime startedAt
) {}
