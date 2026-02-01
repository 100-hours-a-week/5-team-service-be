package com.example.doktoribackend.user.service;

import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.s3.ImageUrlResolver;
import com.example.doktoribackend.s3.service.FileService;
import com.example.doktoribackend.user.domain.Gender;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.preference.UserPreference;
import com.example.doktoribackend.user.dto.ProfileRequiredInfoRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.mapper.UserMapper;
import com.example.doktoribackend.user.repository.UserPreferenceRepository;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        return UserMapper.toUserProfileResponse(user, imageUrlResolver);
    }

    @Transactional(readOnly = true)
    public boolean getNotificationAgreement(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);
        UserPreference preference = user.getUserPreference();
        return preference != null && preference.isNotificationAgreement();
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        String oldImagePath = user.getProfileImagePath();
        String newImagePath = request.profileImagePath();
        fileService.deleteImageIfChanged(oldImagePath, newImagePath);

        user.updateNickname(request.nickname());
        user.updateProfileImage(newImagePath);
        user.updateLeaderIntro(request.leaderIntro());
        user.updateMemberIntro(request.memberIntro());

        return UserMapper.toUserProfileResponse(user, imageUrlResolver);
    }

    @Transactional
    public UserProfileResponse updateProfileRequiredInfo(Long userId, ProfileRequiredInfoRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.isProfileCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_COMPLETED);
        }

        UserPreference preference = user.getUserPreference();
        if (preference == null) {
            preference = UserPreference.builder()
                    .user(user)
                    .gender(request.gender())
                    .birthYear(request.birthYear())
                    .build();
            preference.changeNotificationAgreement(request.notificationAgreement());
            user.linkPreference(preference);
            userPreferenceRepository.save(preference);
        } else {
            preference.updateRequiredInfo(request.gender(), request.birthYear(), request.notificationAgreement());
        }
        user.completeProfile();
        return UserMapper.toUserProfileResponse(user, imageUrlResolver);
    }

    @Transactional
    public void updateNotificationAgreement(Long userId, boolean notificationAgreement) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);
        UserPreference preference = user.getUserPreference();
        if (preference == null) {
            preference = UserPreference.builder()
                    .user(user)
                    .gender(Gender.UNKNOWN)
                    .birthYear(0)
                    .build();
            user.linkPreference(preference);
            userPreferenceRepository.save(preference);
            return;
        }
        if (preference.isNotificationAgreement() != notificationAgreement) {
            preference.changeNotificationAgreement(notificationAgreement);
        }
    }
}
