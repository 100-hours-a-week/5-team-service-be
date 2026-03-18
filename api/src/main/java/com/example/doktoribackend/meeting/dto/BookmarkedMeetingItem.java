package com.example.doktoribackend.meeting.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkedMeetingItem {
    private final Long meetingId;
    private final String meetingImagePath;
    private final String title;
    private final Long readingGenreId;
    private final String leaderNickname;
    private final Integer capacity;
    private final Integer currentMemberCount;
    private final Long remainingDays;
    private final Boolean isRecruiting;
}