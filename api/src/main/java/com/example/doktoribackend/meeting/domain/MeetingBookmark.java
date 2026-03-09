package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.meeting.domain.id.MeetingBookmarkId;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_bookmarks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingBookmark {

    @EmbeddedId
    private MeetingBookmarkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("meetingId")
    @JoinColumn(name = "meeting_id")
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
        bookmark.id = new MeetingBookmarkId(user.getId(), meeting.getId());
        return bookmark;
    }
}