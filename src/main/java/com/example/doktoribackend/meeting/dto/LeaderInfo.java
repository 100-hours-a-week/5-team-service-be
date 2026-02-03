package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaderInfo {
    private String nickname;
    private String profileImagePath;
    private String intro;

    public static LeaderInfo from(User user, String leaderIntro) {
        return LeaderInfo.builder()
                .nickname(user.getNickname())
                .profileImagePath(user.getProfileImagePath())
                .intro(leaderIntro)
                .build();
    }
}