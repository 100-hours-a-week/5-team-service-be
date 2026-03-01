package com.example.doktoribackend.room.dto;

import java.time.LocalDateTime;

public record NextRoundResponse(
        int currentRound,
        LocalDateTime startedAt
) {}
