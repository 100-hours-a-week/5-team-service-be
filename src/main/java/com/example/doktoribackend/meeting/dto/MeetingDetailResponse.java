package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MeetingDetailResponse {
    private MeetingInfo meeting;
    private List<RoundInfo> rounds;
    private ParticipantsPreview participantsPreview;

    public static MeetingDetailResponse from(
            Meeting meeting,
            List<MeetingRound> rounds,
            List<MeetingMember> approvedMembers,
            String myParticipationStatus
    ) {
        return MeetingDetailResponse.builder()
                .meeting(MeetingInfo.from(meeting))
                .rounds(rounds.stream()
                        .map(RoundInfo::from)
                        .toList())
                .participantsPreview(ParticipantsPreview.from(approvedMembers, myParticipationStatus))
                .build();
    }
}