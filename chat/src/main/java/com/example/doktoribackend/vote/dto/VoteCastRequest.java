package com.example.doktoribackend.vote.dto;

import com.example.doktoribackend.room.domain.Position;
import jakarta.validation.constraints.NotNull;

public record VoteCastRequest(
        @NotNull(message = "투표 선택은 필수입니다.")
        Position choice
) {
}
