package com.example.doktoribackend.vote.domain;

import com.example.doktoribackend.room.domain.Position;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote_casts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteCast {

    @EmbeddedId
    private VoteCastId id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Vote vote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position choice;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public static VoteCast create(Vote vote, Long userId, Position choice) {
        return new VoteCast(vote, userId, choice);
    }

    @Builder
    private VoteCast(Vote vote, Long userId, Position choice) {
        this.id = new VoteCastId(vote.getRoomId(), userId);
        this.vote = vote;
        this.choice = choice;
    }
}
