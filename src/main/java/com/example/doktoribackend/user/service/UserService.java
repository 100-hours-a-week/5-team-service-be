package com.example.doktoribackend.user.service;

import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.mapper.UserMapper;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        return UserMapper.toUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        user.updateNickname(request.nickname());
        user.updateProfileImage(request.profileImagePath());
        user.updateLeaderIntro(request.leaderIntro());
        user.updateMemberIntro(request.memberIntro());

        return UserMapper.toUserProfileResponse(user);
    }
}