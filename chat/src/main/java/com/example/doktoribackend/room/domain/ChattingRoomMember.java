package com.example.doktoribackend.room.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "chatting_room_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chatting_room_members_room_user",
        columnNames = {"room_id", "user_id"}
    ),
    indexes = @Index(
        name = "idx_chatting_room_members_room_status_user",
        columnList = "room_id, status, user_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChattingRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChattingRoom chattingRoom;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Position position;

    @Builder
    public ChattingRoomMember(ChattingRoom chattingRoom, Long userId, MemberRole role, Position position) {
        this.chattingRoom = chattingRoom;
        this.userId = userId;
        this.role = role;
        this.position = position;
    }

    public void join() {
        this.status = MemberStatus.JOINED;
    }

    public void disconnect() {
        this.status = MemberStatus.DISCONNECTED;
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
    }

    public boolean isHost() {
        return this.role == MemberRole.HOST;
    }
}
