package com.example.doktoribackend.user.mapper;

import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.dto.UserProfileResponse;


public final class UserMapper {

    private UserMapper() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static UserProfileResponse toUserProfileResponse(User user, ImageUrlResolver resolver) {

        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                resolver.toUrl(user.getProfileImagePath()),
                user.getProfileImagePath(),
                user.isProfileCompleted(),
                user.isOnboardingCompleted(),
                user.getLeaderIntro(),
                user.getMemberIntro()
        );
    }
}