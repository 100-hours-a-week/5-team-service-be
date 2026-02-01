package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class MeetingInfo {
    private Long meetingId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private LocalDateTime createdAt;

    private String status;
    private String meetingImagePath;
    private String title;
    private String description;
    private Long readingGenreId;
    private Integer capacity;
    private Integer currentCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recruitmentDeadline;

    private Integer roundCount;
    private TimeInfo time;
    private LeaderInfo leader;

    public static MeetingInfo from(Meeting meeting) {
        return MeetingInfo.builder()
                .meetingId(meeting.getId())
                .createdAt(meeting.getCreatedAt())
                .status(meeting.getStatus().name())
                .meetingImagePath(meeting.getMeetingImagePath())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .readingGenreId(meeting.getReadingGenreId())
                .capacity(meeting.getCapacity())
                .currentCount(meeting.getCurrentCount())
                .recruitmentDeadline(meeting.getRecruitmentDeadline())
                .roundCount(meeting.getRoundCount())
                .time(TimeInfo.from(meeting.getStartTime(), meeting.getDurationMinutes()))
                .leader(LeaderInfo.from(meeting.getLeaderUser(), meeting.getLeaderIntro()))
                .build();
    }
}