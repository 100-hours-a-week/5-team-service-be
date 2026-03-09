package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.domain.MeetingBookmark;
import com.example.doktoribackend.meeting.domain.id.MeetingBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingBookmarkRepository extends JpaRepository<MeetingBookmark, MeetingBookmarkId> {

    boolean existsById(MeetingBookmarkId id);

    void deleteById(MeetingBookmarkId id);
}