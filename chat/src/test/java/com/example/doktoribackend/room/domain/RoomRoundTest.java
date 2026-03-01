package com.example.doktoribackend.room.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoomRoundTest {

    @Test
    @DisplayName("Builder로 라운드를 생성하면 startedAt이 현재 시간으로 설정된다")
    void create() {
        LocalDateTime before = LocalDateTime.now();

        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        RoomRound round = RoomRound.builder()
                .chattingRoom(room)
                .roundNumber(1)
                .build();

        LocalDateTime after = LocalDateTime.now();

        assertThat(round.getChattingRoom()).isEqualTo(room);
        assertThat(round.getRoundNumber()).isEqualTo(1);
        assertThat(round.getStartedAt()).isBetween(before, after);
        assertThat(round.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("endRound 호출 시 endedAt이 현재 시간으로 설정된다")
    void endRound() {
        ChattingRoom room = ChattingRoom.builder()
                .topic("주제")
                .description("설명")
                .capacity(4)
                .build();

        RoomRound round = RoomRound.builder()
                .chattingRoom(room)
                .roundNumber(1)
                .build();

        LocalDateTime before = LocalDateTime.now();
        round.endRound();
        LocalDateTime after = LocalDateTime.now();

        assertThat(round.getEndedAt()).isBetween(before, after);
    }
}
