package com.example.doktoribackend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeetingListItem {
    private final Long meetingId;
    private final String meetingImagePath;
    private final String title;
    private final Long readingGenreId;
    private final String leaderNickname;
    private final Integer capacity;
    private final Integer currentMemberCount;
}
