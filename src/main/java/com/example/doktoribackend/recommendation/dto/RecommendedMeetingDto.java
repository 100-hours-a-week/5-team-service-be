package com.example.doktoribackend.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class RecommendedMeetingDto {
    private Long meetingId;
    private String meetingImagePath;
    private String title;
    private String readingGenreName;
    private String leaderNickname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recruitmentDeadline;
}