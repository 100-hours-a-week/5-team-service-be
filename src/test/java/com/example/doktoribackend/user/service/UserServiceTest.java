package com.example.doktoribackend.user.service;

import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("getMyProfile: 존재하는 사용자 프로필을 반환한다")
    void getMyProfile_success() {
        User user = User.builder()
                .nickname("nickname")
                .profileImagePath("/images/profile.png")
                .leaderIntro("leader intro")
                .memberIntro("member intro")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.completeOnboarding();
        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.getMyProfile(1L);

        assertThat(response.nickname()).isEqualTo("nickname");
        assertThat(response.profileImagePath()).isEqualTo("/images/profile.png");
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.leaderIntro()).isEqualTo("leader intro");
        assertThat(response.memberIntro()).isEqualTo("member intro");

        then(userRepository).should().findByIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("getMyProfile: 사용자가 없으면 예외가 발생한다")
    void getMyProfile_notFound_throws() {
        given(userRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateMyProfile: 요청 값으로 닉네임과 소개 정보를 변경한다")
    void updateMyProfile_success() {
        User user = User.builder()
                .nickname("oldNick")
                .profileImagePath("/images/old.png")
                .leaderIntro("old leader")
                .memberIntro("old member")
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "/images/new.png",
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(5L)).willReturn(Optional.of(user));

        UserProfileResponse response = userService.updateMyProfile(5L, request);

        assertThat(user.getNickname()).isEqualTo("newNick");
        assertThat(user.getProfileImagePath()).isEqualTo("/images/new.png");
        assertThat(user.getLeaderIntro()).isEqualTo("new leader");
        assertThat(user.getMemberIntro()).isEqualTo("new member");

        assertThat(response.nickname()).isEqualTo("newNick");
        assertThat(response.profileImagePath()).isEqualTo("/images/new.png");
        assertThat(response.leaderIntro()).isEqualTo("new leader");
        assertThat(response.memberIntro()).isEqualTo("new member");

        then(userRepository).should().findByIdAndDeletedAtIsNull(5L);
    }

    @Test
    @DisplayName("updateMyProfile: 사용자가 없으면 예외가 발생한다")
    void updateMyProfile_notFound_throws() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "/images/new.png",
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMyProfile(7L, request))
                .isInstanceOf(UserNotFoundException.class);
    }
}
