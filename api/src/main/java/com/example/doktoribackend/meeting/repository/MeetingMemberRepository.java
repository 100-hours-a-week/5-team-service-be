package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember, Long> {

    @Query("SELECT mm.user.id FROM MeetingMember mm " +
            "WHERE mm.meeting.id = :meetingId " +
            "AND mm.status = 'APPROVED'")
    List<Long> findApprovedMemberUserIds(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserIdAndStatus(Long meetingId, Long userId, MeetingMemberStatus status);

    Optional<MeetingMember> findByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("SELECT mm FROM MeetingMember mm " +
           "JOIN FETCH mm.user " +
           "WHERE mm.meeting.id = :meetingId " +
           "AND mm.status = 'APPROVED' " +
           "ORDER BY mm.createdAt ASC")
    List<MeetingMember> findApprovedMembersByMeetingIdOrderByCreatedAt(@Param("meetingId") Long meetingId);

    @Query("SELECT mm FROM MeetingMember mm " +
           "JOIN FETCH mm.user " +
           "WHERE mm.id = :memberId " +
           "AND mm.meeting.id = :meetingId")
    Optional<MeetingMember> findByIdAndMeetingIdWithUser(
            @Param("memberId") Long memberId,
            @Param("meetingId") Long meetingId
    );

    @Query("SELECT mm FROM MeetingMember mm " +
           "JOIN FETCH mm.meeting m " +
           "WHERE mm.user.id = :userId " +
           "AND mm.role = 'LEADER' " +
           "AND m.status != 'CANCELED' " +
           "AND m.deletedAt IS NULL")
    List<MeetingMember> findActiveLeaderMeetingsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(mm) > 0 THEN true ELSE false END " +
           "FROM MeetingMember mm " +
           "JOIN mm.meeting m " +
           "JOIN MeetingRound mr ON mr.meeting.id = m.id AND mr.roundNo = 1 " +
           "WHERE mm.user.id = :userId " +
           "AND mm.role = 'LEADER' " +
           "AND m.status != 'CANCELED' " +
           "AND m.deletedAt IS NULL " +
           "AND mr.startAt <= :now " +
           "AND EXISTS (" +
           "  SELECT 1 FROM MeetingMember mm2 " +
           "  WHERE mm2.meeting.id = m.id " +
           "  AND mm2.role = 'MEMBER' " +
           "  AND mm2.status IN ('APPROVED', 'PENDING')" +
           ")")
    boolean existsWithdrawalBlockingMeeting(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT mm FROM MeetingMember mm " +
           "JOIN FETCH mm.user " +
           "WHERE mm.meeting.id = :meetingId " +
           "AND mm.status = 'PENDING' " +
           "AND (:cursorId IS NULL OR mm.id < :cursorId) " +
           "ORDER BY mm.id DESC")
    List<MeetingMember> findPendingMembersByMeetingIdWithCursor(
            @Param("meetingId") Long meetingId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

}
