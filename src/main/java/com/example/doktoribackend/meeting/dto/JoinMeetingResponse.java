package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class JoinMeetingResponse {
    private Long joinRequestId;
    private Long meetingId;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
    private Instant requestedAt;

    public static JoinMeetingResponse from(MeetingMember member) {
        return JoinMeetingResponse.builder()
                .joinRequestId(member.getId())
                .meetingId(member.getMeeting().getId())
                .status(member.getStatus().name())
                .requestedAt(member.getCreatedAt())
                .build();
    }
}