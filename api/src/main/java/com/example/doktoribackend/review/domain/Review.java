package com.example.doktoribackend.review.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_round_id", nullable = false)
    private MeetingRound meetingRound;

    @Column(name = "meeting_title", nullable = false, length = 50)
    private String meetingTitle;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "book_title", nullable = false)
    private String bookTitle;

    @Column(name = "meeting_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal meetingRating;

    @Column(name = "leader_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal leaderRating;

    @Column(name = "content", length = 200)
    private String content;

    @Column(name = "best_member_id")
    private Long bestMemberId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images = new ArrayList<>();

    public static Review create(User reviewer,
                                MeetingRound meetingRound,
                                String meetingTitle,
                                Integer roundNo,
                                String bookTitle,
                                BigDecimal meetingRating,
                                BigDecimal leaderRating,
                                String content,
                                Long bestMemberId) {
        Review review = new Review();
        review.reviewer = reviewer;
        review.meetingRound = meetingRound;
        review.meetingTitle = meetingTitle;
        review.roundNo = roundNo;
        review.bookTitle = bookTitle;
        review.meetingRating = meetingRating;
        review.leaderRating = leaderRating;
        review.content = content;
        review.bestMemberId = bestMemberId;
        return review;
    }

    public void addImage(ReviewImage image) {
        images.add(image);
        image.assignReview(this);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
