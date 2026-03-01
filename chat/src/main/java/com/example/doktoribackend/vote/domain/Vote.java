package com.example.doktoribackend.vote.domain;

import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.Position;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote implements Persistable<Long> {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @Transient
    private boolean isNew = true;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "room_id")
    private ChattingRoom chattingRoom;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "total_member_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer totalMemberCount;

    @Column(name = "agree_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer agreeCount = 0;

    @Column(name = "disagree_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer disagreeCount = 0;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL)
    private final List<VoteCast> voteCasts = new ArrayList<>();

    public static Vote create(ChattingRoom chattingRoom, int totalMemberCount) {
        return new Vote(chattingRoom, totalMemberCount);
    }

    @Builder
    private Vote(ChattingRoom chattingRoom, int totalMemberCount) {
        this.roomId = chattingRoom.getId();
        this.chattingRoom = chattingRoom;
        this.totalMemberCount = totalMemberCount;
        this.openedAt = LocalDateTime.now();
    }

    public void close() {
        this.closedAt = LocalDateTime.now();
    }

    public boolean isClosed() {
        return this.closedAt != null;
    }

    public void incrementCount(Position choice) {
        if (choice == Position.AGREE) {
            this.agreeCount++;
        } else {
            this.disagreeCount++;
        }
    }

    @Override
    public Long getId() {
        return roomId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
