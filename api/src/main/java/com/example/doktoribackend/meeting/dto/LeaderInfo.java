package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaderInfo {
    private String nickname;
    private String profileImagePath;
    private String intro;
    private Double averageRating;
    private long leaderMeetingCount;

    public static LeaderInfo from(User user, String leaderIntro, ImageUrlResolver imageUrlResolver,
                                  Double averageRating, long leaderMeetingCount) {
        return LeaderInfo.builder()
                .nickname(user.getNickname())
                .profileImagePath(imageUrlResolver.toUrl(user.getProfileImagePath()))
                .intro(leaderIntro)
                .averageRating(averageRating)
                .leaderMeetingCount(leaderMeetingCount)
                .build();
    }
}