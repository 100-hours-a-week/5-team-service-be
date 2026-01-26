package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_rounds",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_round", columnNames = {"meeting_id", "round_no"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetingRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "round_no", nullable = false, columnDefinition = "TINYINT")
    private Integer roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingRoundStatus status;

    @Column(name = "meeting_link", length = 1024)
    private String meetingLink;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    public static MeetingRound create(
            Meeting meeting,
            Book book,
            Integer roundNo,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return MeetingRound.builder()
                .meeting(meeting)
                .book(book)
                .roundNo(roundNo)
                .status(MeetingRoundStatus.SCHEDULED)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }

    public void updateMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    // 양방향 관계 설정을 위한 메서드
    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
