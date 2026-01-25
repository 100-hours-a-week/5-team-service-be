package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.dto.MeetingListRow;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;

import java.util.List;

public interface MeetingRepositoryCustom {
    List<MeetingListRow> findMeetingList(MeetingListRequest request, int limit);
}
