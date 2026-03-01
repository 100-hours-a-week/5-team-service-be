package com.example.doktoribackend.bookReport.repository;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.dto.BookReportProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookReportRepository extends JpaRepository<BookReport, Long> {

    Optional<BookReport> findByUserIdAndMeetingRoundIdAndDeletedAtIsNull(Long userId, Long meetingRoundId);

    @Query("SELECT br.id AS id, br.status AS status, br.content AS content, br.rejectionReason AS rejectionReason " +
            "FROM BookReport br " +
            "WHERE br.user.id = :userId " +
            "AND br.meetingRound.id = :meetingRoundId " +
            "AND br.deletedAt IS NULL")
    Optional<BookReportProjection> findProjectionByUserIdAndMeetingRoundId(
            @Param("userId") Long userId,
            @Param("meetingRoundId") Long meetingRoundId);

    @Query("SELECT COUNT(br) FROM BookReport br " +
            "WHERE br.user.id = :userId " +
            "AND br.createdAt >= :startOfDay")
    int countTodaySubmissions(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    // 특정 회차의 독후감 목록 조회 (관리 화면용)
    @Query("SELECT br FROM BookReport br " +
            "WHERE br.meetingRound.id = :roundId " +
            "AND br.deletedAt IS NULL")
    List<BookReport> findByMeetingRoundId(@Param("roundId") Long roundId);

    // 특정 모임에서 특정 사용자의 APPROVED 독후감 수 조회
    @Query("SELECT COUNT(br) FROM BookReport br " +
            "JOIN br.meetingRound mr " +
            "WHERE mr.meeting.id = :meetingId " +
            "AND br.user.id = :userId " +
            "AND br.status = 'APPROVED' " +
            "AND br.deletedAt IS NULL")
    int countApprovedByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId);

    // 특정 모임의 모든 멤버별 APPROVED 독후감 수 일괄 조회 (N+1 방지)
    @Query("SELECT br.user.id AS userId, COUNT(br) AS approvedCount " +
            "FROM BookReport br " +
            "JOIN br.meetingRound mr " +
            "WHERE mr.meeting.id = :meetingId " +
            "AND br.status = 'APPROVED' " +
            "AND br.deletedAt IS NULL " +
            "GROUP BY br.user.id")
    List<MemberApprovedCountProjection> countApprovedByMeetingIdGroupByUser(@Param("meetingId") Long meetingId);

    @Query("SELECT br.id AS bookReportId, br.user.id AS userId, " +
            "mr.meeting.id AS meetingId, mr.meeting.title AS meetingTitle " +
            "FROM BookReport br JOIN br.meetingRound mr " +
            "WHERE br.status = 'PENDING_REVIEW' " +
            "AND br.deletedAt IS NULL " +
            "AND br.createdAt <= :threshold")
    List<StaleReportProjection> findStalePendingReportProjections(@Param("threshold") LocalDateTime threshold);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BookReport br " +
            "SET br.status = :rejectedStatus, br.rejectionReason = :reason, br.aiValidatedAt = :now " +
            "WHERE br.status = 'PENDING_REVIEW' " +
            "AND br.deletedAt IS NULL " +
            "AND br.createdAt <= :threshold")
    int bulkRejectStalePendingReports(
            @Param("threshold") LocalDateTime threshold,
            @Param("reason") String reason,
            @Param("now") LocalDateTime now,
            @Param("rejectedStatus") BookReportStatus rejectedStatus);

    interface StaleReportProjection {
        Long getBookReportId();
        Long getUserId();
        Long getMeetingId();
        String getMeetingTitle();
    }

    interface MemberApprovedCountProjection {
        Long getUserId();
        Long getApprovedCount();
    }
}
