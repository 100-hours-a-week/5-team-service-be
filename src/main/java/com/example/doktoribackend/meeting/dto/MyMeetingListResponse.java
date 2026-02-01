package com.example.doktoribackend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyMeetingListResponse {
    private final List<MyMeetingItem> items;
    private final PageInfo pageInfo;
}