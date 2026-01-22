package com.example.doktoribackend.meeting.repository;

import com.example.doktoribackend.meeting.dto.MeetingListItem;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;

import java.util.List;

public interface MeetingRepositoryCustom {
    List<MeetingListItem> findMeetingList(MeetingListRequest request, int limit);
}
