package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingBookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MeetingBookmarkRepository extends JpaRepository<MeetingBookmark, Long> {

    boolean existsByUserIdAndMeetingId(Long userId, Long meetingId);

    Optional<MeetingBookmark> findByUserIdAndMeetingId(Long userId, Long meetingId);

    void deleteByUserIdAndMeetingId(Long userId, Long meetingId);

    @Query("SELECT b FROM MeetingBookmark b " +
            "JOIN FETCH b.meeting m " +
            "JOIN FETCH m.leaderUser " +
            "JOIN FETCH m.readingGenre " +
            "WHERE b.user.id = :userId " +
            "AND (:cursorId IS NULL OR b.id < :cursorId) " +
            "ORDER BY b.id DESC")
    List<MeetingBookmark> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT b.meeting.id FROM MeetingBookmark b WHERE b.user.id = :userId AND b.meeting.id IN :meetingIds")
    Set<Long> findBookmarkedMeetingIds(@Param("userId") Long userId, @Param("meetingIds") List<Long> meetingIds);
}