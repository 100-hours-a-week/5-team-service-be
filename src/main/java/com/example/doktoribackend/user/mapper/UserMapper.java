package com.example.doktoribackend.user.mapper;

import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.dto.UserProfileResponse;


public final class UserMapper {

    private UserMapper() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static UserProfileResponse toUserProfileResponse(User user) {

        return new UserProfileResponse(
                user.getNickname(),
                user.getProfileImagePath(),
                user.isOnboardingCompleted(),
                user.getLeaderIntro(),
                user.getMemberIntro()
        );
    }
}