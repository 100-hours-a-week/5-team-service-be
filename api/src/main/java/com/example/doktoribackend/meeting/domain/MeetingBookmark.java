package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_bookmark", columnNames = {"user_id", "meeting_id"}),
        indexes = @Index(name = "idx_meeting_bookmark_user_created", columnList = "user_id, created_at DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static MeetingBookmark create(User user, Meeting meeting) {
        MeetingBookmark bookmark = new MeetingBookmark();
        bookmark.user = user;
        bookmark.meeting = meeting;
        return bookmark;
    }
}