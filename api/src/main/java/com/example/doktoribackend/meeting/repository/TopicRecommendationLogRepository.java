package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.TopicRecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TopicRecommendationLogRepository extends JpaRepository<TopicRecommendationLog, Long> {

    @Query("SELECT COUNT(l) FROM TopicRecommendationLog l " +
            "WHERE l.meeting.id = :meetingId " +
            "AND l.createdAt >= :startOfDay " +
            "AND l.createdAt < :nextDayStart")
    int countTodayByMeetingId(
            @Param("meetingId") Long meetingId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("nextDayStart") LocalDateTime nextDayStart
    );
}
