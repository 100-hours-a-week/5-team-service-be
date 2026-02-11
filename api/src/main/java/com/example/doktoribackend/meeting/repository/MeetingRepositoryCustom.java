package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.dto.MeetingListRow;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingSearchRequest;

import java.time.LocalDate;
import java.util.List;

public interface MeetingRepositoryCustom {
    List<MeetingListRow> findMeetingList(MeetingListRequest request, int limit);
    List<MeetingListRow> searchMeetings(MeetingSearchRequest request, int limit);
    List<MeetingListRow> findMyMeetings(Long userId, Long cursorId, boolean activeOnly, int limit);
    List<MeetingListRow> findMyTodayMeetings(Long userId, LocalDate today);
}
