package com.example.doktoribackend.vote.dto;

import com.example.doktoribackend.room.domain.Position;

public record VoteResultResponse(
        Integer agreeCount,
        Integer disagreeCount,
        Integer totalMemberCount,
        boolean isClosed,
        Position myChoice
) {
}