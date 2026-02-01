package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
