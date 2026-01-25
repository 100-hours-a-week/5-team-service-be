package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRoundRepository extends JpaRepository<MeetingRound, Long> {

    @Query("SELECT mr.id FROM MeetingRound mr " +
            "JOIN mr.meeting m " +
            "WHERE m.status IN :statuses " +
            "AND mr.startAt = :targetTime " +
            "AND mr.meetingLink IS NULL")
    List<Long> findMeetingRoundIdsForZoomLinkCreation(
            @Param("statuses") List<MeetingStatus> statuses,
            @Param("targetTime") LocalDateTime targetTime
    );

    @Query("SELECT mr FROM MeetingRound mr " +
            "JOIN FETCH mr.meeting " +
            "WHERE mr.id IN :ids")
    List<MeetingRound> findByIdsWithMeeting(@Param("ids") List<Long> ids);
}
