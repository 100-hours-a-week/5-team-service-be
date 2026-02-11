package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingMember;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.s3.ImageUrlResolver;
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
            String myParticipationStatus,
            ImageUrlResolver imageUrlResolver
    ) {
        return MeetingDetailResponse.builder()
                .meeting(MeetingInfo.from(meeting, imageUrlResolver))
                .rounds(rounds.stream()
                        .map(RoundInfo::from)
                        .toList())
                .participantsPreview(ParticipantsPreview.from(approvedMembers, myParticipationStatus, imageUrlResolver))
                .build();
    }
}