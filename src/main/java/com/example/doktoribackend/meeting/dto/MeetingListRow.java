package com.example.doktoribackend.meeting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class MeetingListRow {
    private final Long meetingId;
    private final String meetingImagePath;
    private final String title;
    private final Long readingGenreId;
    private final String leaderNickname;
    private final Integer capacity;
    private final Integer currentMemberCount;
    private final LocalDate recruitmentDeadline;
}
