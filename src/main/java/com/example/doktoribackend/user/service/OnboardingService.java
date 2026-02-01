package com.example.doktoribackend.user.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.reading.repository.ReadingGenreRepository;
import com.example.doktoribackend.user.domain.Gender;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.preference.UserPreference;
import com.example.doktoribackend.user.domain.preference.UserReadingGenre;
import com.example.doktoribackend.user.domain.preference.UserReadingPurpose;
import com.example.doktoribackend.user.dto.OnboardingRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.mapper.UserMapper;
import com.example.doktoribackend.user.policy.ReadingPurpose;
import com.example.doktoribackend.user.policy.ReadingVolume;
import com.example.doktoribackend.user.repository.ReadingPurposeRepository;
import com.example.doktoribackend.user.repository.ReadingVolumeRepository;
import com.example.doktoribackend.user.repository.UserPreferenceRepository;
import com.example.doktoribackend.user.repository.UserReadingGenreRepository;
import com.example.doktoribackend.user.repository.UserReadingPurposeRepository;
import com.example.doktoribackend.user.repository.UserRepository;
import com.example.doktoribackend.s3.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ReadingVolumeRepository readingVolumeRepository;
    private final ReadingPurposeRepository readingPurposeRepository;
    private final ReadingGenreRepository readingGenreRepository;
    private final UserReadingPurposeRepository userReadingPurposeRepository;
    private final UserReadingGenreRepository userReadingGenreRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public UserProfileResponse onboard(Long userId, OnboardingRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        UserPreference preference = resolvePreference(user);

        if (request.readingVolumeId() != null) {
            ReadingVolume readingVolume = readingVolumeRepository
                    .findByIdAndDeletedAtIsNull(request.readingVolumeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
            preference.updateOnboardingInfo(readingVolume);
        }

        updateReadingPurposes(user, request.readingPurposeIds());
        updateReadingGenres(user, request.readingGenreIds());

        user.completeOnboarding();

        return UserMapper.toUserProfileResponse(user, imageUrlResolver);
    }

    private UserPreference resolvePreference(User user) {
        UserPreference preference = user.getUserPreference();
        if (preference != null) {
            return preference;
        }

        UserPreference created = UserPreference.builder()
                .user(user)
                .gender(Gender.UNKNOWN)
                .birthYear(0)
                .build();
        user.linkPreference(created);
        return userPreferenceRepository.save(created);
    }

    private void updateReadingPurposes(User user, List<Long> readingPurposeIds) {
        if (readingPurposeIds == null) {
            return;
        }

        userReadingPurposeRepository.deleteByUser(user);

        if (readingPurposeIds.isEmpty()) {
            return;
        }

        List<Long> ids = distinctIds(readingPurposeIds);
        List<ReadingPurpose> readingPurposes = readingPurposeRepository.findAllByIdInAndDeletedAtIsNull(ids);
        if (readingPurposes.size() != ids.size()) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        List<UserReadingPurpose> userReadingPurposes = readingPurposes.stream()
                .map(readingPurpose -> UserReadingPurpose.create(user, readingPurpose))
                .toList();

        userReadingPurposeRepository.saveAll(userReadingPurposes);
    }

    private void updateReadingGenres(User user, List<Long> readingGenreIds) {
        if (readingGenreIds == null) {
            return;
        }

        userReadingGenreRepository.deleteByUser(user);

        if (readingGenreIds.isEmpty()) {
            return;
        }

        List<Long> ids = distinctIds(readingGenreIds);
        List<ReadingGenre> readingGenres = readingGenreRepository.findAllByIdInAndDeletedAtIsNull(ids);
        if (readingGenres.size() != ids.size()) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }

        List<UserReadingGenre> userReadingGenres = readingGenres.stream()
                .map(readingGenre -> UserReadingGenre.create(user, readingGenre))
                .toList();

        userReadingGenreRepository.saveAll(userReadingGenres);
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
