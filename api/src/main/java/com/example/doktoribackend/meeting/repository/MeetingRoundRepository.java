package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.meeting.domain.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingRoundRepository extends JpaRepository<MeetingRound, Long> {

    @Query("SELECT mr.id FROM MeetingRound mr " +
            "JOIN mr.meeting m " +
            "WHERE m.status IN :statuses " +
            "AND mr.startAt > :now " +
            "AND mr.startAt <= :targetTime " +
            "AND mr.meetingLink IS NULL")
    List<Long> findMeetingRoundIdsForZoomLinkCreation(
            @Param("statuses") List<MeetingStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("targetTime") LocalDateTime targetTime
    );

    @Query("SELECT mr FROM MeetingRound mr " +
            "JOIN FETCH mr.meeting " +
            "WHERE mr.id IN :ids")
    List<MeetingRound> findByIdsWithMeeting(@Param("ids") List<Long> ids);

    @Query("SELECT mr FROM MeetingRound mr " +
            "JOIN FETCH mr.meeting m " +
            "WHERE mr.startAt > :from " +
            "AND mr.startAt <= :to " +
            "AND m.status IN ('RECRUITING', 'FINISHED')")
    List<MeetingRound> findRoundsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    @Query("SELECT mr FROM MeetingRound mr " +
           "JOIN FETCH mr.book " +
           "WHERE mr.meeting.id = :meetingId " +
           "ORDER BY mr.roundNo ASC")
    List<MeetingRound> findByMeetingIdWithBook(@Param("meetingId") Long meetingId);

    @Query("SELECT mr FROM MeetingRound mr " +
            "WHERE mr.meeting.id = :meetingId " +
            "AND mr.roundNo = :roundNo")
    Optional<MeetingRound> findByMeetingIdAndRoundNo(
            @Param("meetingId") Long meetingId,
            @Param("roundNo") Integer roundNo
    );

    @Query("SELECT mr FROM MeetingRound mr " +
            "JOIN FETCH mr.book " +
            "JOIN FETCH mr.meeting " +
            "WHERE mr.id = :id")
    Optional<MeetingRound> findByIdWithBookAndMeeting(@Param("id") Long id);

    // 나의 모임 리스트: 다음 회차 날짜 조회 (단일 모임)
    @Query("SELECT mr.startAt FROM MeetingRound mr " +
           "WHERE mr.meeting.id = :meetingId " +
           "AND mr.startAt >= :now " +
           "ORDER BY mr.startAt ASC")
    List<LocalDateTime> findNextRoundDate(@Param("meetingId") Long meetingId, @Param("now") LocalDateTime now);

    // 나의 모임 리스트: 다음 회차 날짜 일괄 조회 (여러 모임) - N+1 해결
    @Query("SELECT mr.meeting.id as meetingId, MIN(mr.startAt) as nextRoundDate " +
           "FROM MeetingRound mr " +
           "WHERE mr.meeting.id IN :meetingIds " +
           "AND mr.startAt >= :now " +
           "GROUP BY mr.meeting.id")
    List<NextRoundProjection> findNextRoundDatesByMeetingIds(
            @Param("meetingIds") List<Long> meetingIds,
            @Param("now") LocalDateTime now
    );

    // 아직 종료되지 않은 회차가 있는 모임 ID 목록 조회
    @Query("SELECT DISTINCT mr.meeting.id FROM MeetingRound mr " +
           "WHERE mr.meeting.id IN :meetingIds " +
           "AND mr.endAt > :now")
    List<Long> findMeetingIdsWithOngoingRounds(
            @Param("meetingIds") List<Long> meetingIds,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE MeetingRound mr SET mr.status = 'DONE' " +
            "WHERE mr.status = 'SCHEDULED' " +
            "AND mr.endAt < :now")
    int bulkUpdateExpiredToDone(@Param("now") LocalDateTime now);
}
