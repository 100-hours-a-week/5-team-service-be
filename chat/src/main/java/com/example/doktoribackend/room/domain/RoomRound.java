package com.example.doktoribackend.room.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "room_rounds",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_room_rounds_room_round",
        columnNames = {"room_id", "round_number"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChattingRoom chattingRoom;

    @Column(name = "round_number", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer roundNumber;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Builder
    public RoomRound(ChattingRoom chattingRoom, Integer roundNumber) {
        this.chattingRoom = chattingRoom;
        this.roundNumber = roundNumber;
        this.startedAt = LocalDateTime.now();
    }

    public void endRound() {
        this.endedAt = LocalDateTime.now();
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
