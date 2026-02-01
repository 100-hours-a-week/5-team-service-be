package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JoinMeetingResponse {
    private Long joinRequestId;
    private Long meetingId;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime requestedAt;

    public static JoinMeetingResponse from(MeetingMember member) {
        return JoinMeetingResponse.builder()
                .joinRequestId(member.getId())
                .meetingId(member.getMeeting().getId())
                .status(member.getStatus().name())
                .requestedAt(member.getCreatedAt())
                .build();
    }
}
