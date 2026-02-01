package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Query("SELECT m FROM Meeting m " +
            "JOIN FETCH m.leaderUser " +
            "WHERE m.id = :meetingId")
    Optional<Meeting> findByIdWithLeader(@Param("meetingId") Long meetingId);

    @Query("SELECT m FROM Meeting m " +
            "WHERE m.status = 'RECRUITING' " +
            "AND m.recruitmentDeadline < :today " +
            "AND m.deletedAt IS NULL")
    List<Meeting> findExpiredRecruitingMeetings(@Param("today") LocalDate today);
}
