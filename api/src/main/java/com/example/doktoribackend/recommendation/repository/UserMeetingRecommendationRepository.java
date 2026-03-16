package com.example.doktoribackend.recommendation.repository;

import com.example.doktoribackend.recommendation.domain.UserMeetingRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UserMeetingRecommendationRepository extends JpaRepository<UserMeetingRecommendation, Long> {

    @Query("""
            SELECT r
            FROM UserMeetingRecommendation r
            JOIN FETCH r.meeting m
            JOIN FETCH m.leaderUser
            WHERE r.user.id = :userId
              AND r.weekStartDate = :weekStartDate
              AND m.status = 'RECRUITING'
              AND m.deletedAt IS NULL
            ORDER BY r.rank ASC
            """)
    List<UserMeetingRecommendation> findByUserIdAndWeekStartDateOrderByRank(
            @Param("userId") Long userId,
            @Param("weekStartDate") LocalDate weekStartDate
    );

    @Query("""
            SELECT r
            FROM UserMeetingRecommendation r
            JOIN FETCH r.meeting m
            JOIN FETCH m.leaderUser
            WHERE m.status = 'RECRUITING'
              AND m.deletedAt IS NULL
              AND r.id = (
                  SELECT MIN(r2.id)
                  FROM UserMeetingRecommendation r2
                  WHERE r2.meeting.id = m.id
              )
            ORDER BY m.createdAt DESC
            """)
    List<UserMeetingRecommendation> findRecruitingMeetingsDistinct();
}