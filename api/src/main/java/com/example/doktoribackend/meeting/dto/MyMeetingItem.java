package com.example.doktoribackend.meeting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MyMeetingItem {
    private Long meetingId;
    private String meetingImagePath;
    private String title;
    private Long readingGenreId;
    private String leaderNickname;
    private Integer currentRound;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;  // 다음 회차 날짜
}