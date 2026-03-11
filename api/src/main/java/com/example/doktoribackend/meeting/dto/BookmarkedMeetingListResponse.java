package com.example.doktoribackend.meeting.dto;

import java.util.List;

public record BookmarkedMeetingListResponse(List<BookmarkedMeetingItem> items, PageInfo pageInfo) {
}