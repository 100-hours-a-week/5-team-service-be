package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetailResponse {
    private MeetingInfo meeting;
    private List<RoundInfo> rounds;

    public static MeetingDetailResponse from(
            Meeting meeting,
            List<MeetingRound> rounds,
            ImageUrlResolver imageUrlResolver,
            Double averageRating,
            long leaderMeetingCount
    ) {
        return MeetingDetailResponse.builder()
                .meeting(MeetingInfo.from(meeting, imageUrlResolver, averageRating, leaderMeetingCount))
                .rounds(rounds.stream()
                        .map(RoundInfo::from)
                        .toList())
                .build();
    }
}