package com.example.doktoribackend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MeetingListResponse {
    private final List<MeetingListItem> items;
    private final PageInfo pageInfo;
}
