package com.example.doktoribackend.bookReport.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookReport extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_round_id", nullable = false)
    private MeetingRound meetingRound;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookReportStatus status = BookReportStatus.PENDING_REVIEW;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(name = "ai_validated_at")
    private LocalDateTime aiValidatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static BookReport create(User user, MeetingRound meetingRound, String content) {
        BookReport bookReport = new BookReport();
        bookReport.user = user;
        bookReport.meetingRound = meetingRound;
        bookReport.content = content;
        bookReport.status = BookReportStatus.PENDING_REVIEW;
        return bookReport;
    }

    public boolean isResubmittable() {
        return this.status == BookReportStatus.REJECTED;
    }

    public void approve() {
        this.status = BookReportStatus.APPROVED;
        this.aiValidatedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(String rejectionReason) {
        this.status = BookReportStatus.REJECTED;
        this.aiValidatedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
