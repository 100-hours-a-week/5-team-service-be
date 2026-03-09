package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingBookmarkRepository extends JpaRepository<MeetingBookmark, Long> {

    boolean existsByUserIdAndMeetingId(Long userId, Long meetingId);

    Optional<MeetingBookmark> findByUserIdAndMeetingId(Long userId, Long meetingId);

    void deleteByUserIdAndMeetingId(Long userId, Long meetingId);
}