package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public static MeetingInfo from(Meeting meeting, ImageUrlResolver imageUrlResolver,
                                   Double averageRating, long leaderMeetingCount) {
        return MeetingInfo.builder()
                .meetingId(meeting.getId())
                .createdAt(meeting.getCreatedAt())
                .status(meeting.getStatus().name())
                .meetingImagePath(imageUrlResolver.toUrl(meeting.getMeetingImagePath()))
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .readingGenreId(meeting.getReadingGenreId())
                .capacity(meeting.getCapacity())
                .currentCount(meeting.getCurrentCount())
                .recruitmentDeadline(meeting.getRecruitmentDeadline())
                .roundCount(meeting.getRoundCount())
                .time(TimeInfo.from(meeting.getStartTime(), meeting.getDurationMinutes()))
                .leader(LeaderInfo.from(meeting.getLeaderUser(), meeting.getLeaderIntro(), imageUrlResolver,
                        averageRating, leaderMeetingCount))
                .build();
    }
}