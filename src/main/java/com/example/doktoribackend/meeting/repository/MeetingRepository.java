package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long>, MeetingRepositoryCustom {

    @Query("SELECT m FROM Meeting m " +
           "JOIN FETCH m.leaderUser " +
           "WHERE m.id = :meetingId")
    Optional<Meeting> findByIdWithLeader(@Param("meetingId") Long meetingId);
}
