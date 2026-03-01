package com.example.doktoribackend.meeting.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_member", columnNames = {"meeting_id", "user_id"}),
        indexes = {
                @Index(name = "idx_meeting_member_meeting_status_user", columnList = "meeting_id,status,user_id"),
                @Index(name = "idx_meeting_member_user_status_meeting", columnList = "user_id,status,meeting_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetingMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingMemberStatus status;

    @Column(name = "member_intro", length = 300)
    private String memberIntro;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public static MeetingMember createLeader(Meeting meeting, User user, LocalDateTime approvedAt) {
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .role(MeetingMemberRole.LEADER)
                .status(MeetingMemberStatus.APPROVED)
                .approvedAt(approvedAt)
                .build();
    }

    public static MeetingMember createParticipant(Meeting meeting, User user) {
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .role(MeetingMemberRole.MEMBER)
                .status(MeetingMemberStatus.PENDING)
                .memberIntro(user.getMemberIntro())
                .build();
    }

    public boolean isPending() {
        return this.status == MeetingMemberStatus.PENDING;
    }

    public boolean belongsTo(Long meetingId) {
        return this.meeting != null && this.meeting.getId().equals(meetingId);
    }

    public void approve(LocalDateTime approvedAt) {
        this.status = MeetingMemberStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    public void reject(LocalDateTime rejectedAt) {
        this.status = MeetingMemberStatus.REJECTED;
        this.rejectedAt = rejectedAt;
    }

    public void cancel(LocalDateTime leftAt) {
        this.status = MeetingMemberStatus.LEFT;
        this.leftAt = leftAt;
    }

    public void kick(LocalDateTime leftAt) {
        this.status = MeetingMemberStatus.KICKED;
        this.leftAt = leftAt;
    }

    public boolean isLeader() {
        return this.role == MeetingMemberRole.LEADER;
    }

    public boolean isApproved() {
        return this.status == MeetingMemberStatus.APPROVED;
    }

    public void promoteToLeader() {
        this.role = MeetingMemberRole.LEADER;
    }

    public void demoteToMember() {
        this.role = MeetingMemberRole.MEMBER;
    }
}
