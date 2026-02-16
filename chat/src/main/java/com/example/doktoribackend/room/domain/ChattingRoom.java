package com.example.doktoribackend.room.domain;

import com.example.doktoribackend.quiz.domain.Quiz;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "chatting_rooms",
    indexes = @Index(name = "idx_chatting_rooms_status_id", columnList = "status, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChattingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String topic;

    @Column(nullable = false, length = 50)
    private String description;

    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer capacity;

    @Column(name = "current_member_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer currentMemberCount = 0;

    @Column(nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private Integer duration = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.WAITING;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "chattingRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Quiz quiz;

    @OneToMany(mappedBy = "chattingRoom", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private final List<RoomRound> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "chattingRoom", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private final List<ChattingRoomMember> members = new ArrayList<>();

    public static ChattingRoom create(ChatRoomCreateRequest request) {
        return new ChattingRoom(request.topic(), request.description(), request.capacity(), null);
    }

    @Builder
    private ChattingRoom(String topic, String description, Integer capacity, Integer duration) {
        this.topic = topic;
        this.description = description;
        this.capacity = capacity;
        this.duration = duration != null ? duration : 30;
    }

    public void startChatting() {
        this.status = RoomStatus.CHATTING;
    }

    public void endChatting() {
        this.status = RoomStatus.ENDED;
    }

    public void cancel() {
        this.status = RoomStatus.CANCELLED;
    }

    public void increaseMemberCount() {
        this.currentMemberCount++;
    }

    public void decreaseMemberCount() {
        if (this.currentMemberCount > 0) {
            this.currentMemberCount--;
        }
    }

    public void linkQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}
