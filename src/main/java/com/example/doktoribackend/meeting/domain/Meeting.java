package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "meetings", indexes = {
        @Index(name = "idx_meeting_list", columnList = "status,deleted_at,id"),
        @Index(name = "idx_meeting_genre_status", columnList = "reading_genre_id,status,deleted_at,id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User leaderUser;

    @Column(name = "reading_genre_id", nullable = false)
    private Long readingGenreId;

    @Column(name = "leader_intro", length = 300)
    private String leaderIntro;

    @Column(name = "meeting_image_path", length = 512, nullable = false)
    private String meetingImagePath;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false)
    private Byte capacity;

    @Column(name = "current_count", nullable = false)
    private Byte currentCount;

    @Column(name = "round_count", nullable = false)
    private Byte roundCount;

    @Column(name = "current_round", nullable = false)
    private Byte currentRound;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 3)
    private MeetingDayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    @Column(name = "first_round_at", nullable = false)
    private LocalDateTime firstRoundAt;

    @Column(name = "recruitment_deadline", nullable = false)
    private LocalDate recruitmentDeadline;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Meeting create(
            User leaderUser,
            Long readingGenreId,
            String leaderIntro,
            String meetingImagePath,
            String title,
            String description,
            Byte capacity,
            Byte roundCount,
            MeetingDayOfWeek dayOfWeek,
            LocalTime startTime,
            Short durationMinutes,
            LocalDateTime firstRoundAt,
            LocalDate recruitmentDeadline,
            Byte currentCount
    ) {
        return Meeting.builder()
                .leaderUser(leaderUser)
                .readingGenreId(readingGenreId)
                .leaderIntro(leaderIntro)
                .meetingImagePath(meetingImagePath)
                .title(title)
                .description(description)
                .capacity(capacity)
                .currentCount(currentCount)
                .roundCount(roundCount)
                .currentRound((byte) 1)
                .status(MeetingStatus.RECRUITING)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .durationMinutes(durationMinutes)
                .firstRoundAt(firstRoundAt)
                .recruitmentDeadline(recruitmentDeadline)
                .build();
    }
}
