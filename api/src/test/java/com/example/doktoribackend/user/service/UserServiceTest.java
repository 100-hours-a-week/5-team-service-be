package com.example.doktoribackend.user.service;

import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.s3.ImageUrlResolver;
import com.example.doktoribackend.s3.service.FileService;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.repository.UserPreferenceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserPreferenceRepository userPreferenceRepository;

    @Mock
    ImageUrlResolver imageUrlResolver;

    @Mock
    FileService fileService;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("getMyProfile: 존재하는 사용자 프로필을 반환한다")
    void getMyProfile_success() {
        // given
        User user = User.builder()
                .nickname("nickname")
                .profileImagePath("images/profiles/test.png")
                .leaderIntro("leader intro")
                .memberIntro("member intro")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.completeProfile();

        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("images/profiles/test.png"))
                .willReturn("https://bucket.s3.amazonaws.com/images/profiles/test.png");

        // when
        UserProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response.nickname()).isEqualTo("nickname");
        assertThat(response.profileImagePath()).isEqualTo("https://bucket.s3.amazonaws.com/images/profiles/test.png");
        assertThat(response.profileImageKey()).isEqualTo("images/profiles/test.png");
        assertThat(response.profileCompleted()).isTrue();
        assertThat(response.leaderIntro()).isEqualTo("leader intro");
        assertThat(response.memberIntro()).isEqualTo("member intro");

        then(userRepository).should().findByIdAndDeletedAtIsNull(1L);
        then(imageUrlResolver).should().toUrl("images/profiles/test.png");
    }

    @Test
    @DisplayName("getMyProfile: 외부 URL(카카오 프로필)은 그대로 반환한다")
    void getMyProfile_withExternalUrl_success() {
        // given
        User user = User.builder()
                .nickname("kakaoUser")
                .profileImagePath("https://k.kakaocdn.net/dn/profile/default.jpg")
                .leaderIntro("leader intro")
                .memberIntro("member intro")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("https://k.kakaocdn.net/dn/profile/default.jpg"))
                .willReturn("https://k.kakaocdn.net/dn/profile/default.jpg");

        // when
        UserProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response.profileImagePath()).isEqualTo("https://k.kakaocdn.net/dn/profile/default.jpg");
        then(imageUrlResolver).should().toUrl("https://k.kakaocdn.net/dn/profile/default.jpg");
    }

    @Test
    @DisplayName("getMyProfile: 사용자가 없으면 예외가 발생한다")
    void getMyProfile_notFound_throws() {
        // given
        given(userRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateMyProfile: 요청 값으로 프로필 정보를 변경한다")
    void updateMyProfile_success() {
        // given
        User user = User.builder()
                .nickname("oldNick")
                .profileImagePath("images/profiles/old.png")
                .leaderIntro("old leader")
                .memberIntro("old member")
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "images/profiles/new.png",
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(5L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("images/profiles/new.png"))
                .willReturn("https://bucket.s3.amazonaws.com/images/profiles/new.png");

        // when
        UserProfileResponse response = userService.updateMyProfile(5L, request);

        // then
        assertThat(user.getNickname()).isEqualTo("newNick");
        assertThat(user.getProfileImagePath()).isEqualTo("images/profiles/new.png");
        assertThat(user.getLeaderIntro()).isEqualTo("new leader");
        assertThat(user.getMemberIntro()).isEqualTo("new member");

        assertThat(response.nickname()).isEqualTo("newNick");
        assertThat(response.profileImagePath()).isEqualTo("https://bucket.s3.amazonaws.com/images/profiles/new.png");
        assertThat(response.leaderIntro()).isEqualTo("new leader");
        assertThat(response.memberIntro()).isEqualTo("new member");

        then(userRepository).should().findByIdAndDeletedAtIsNull(5L);
        then(fileService).should().deleteImageIfChanged("images/profiles/old.png", "images/profiles/new.png");
        then(imageUrlResolver).should().toUrl("images/profiles/new.png");
    }

    @Test
    @DisplayName("updateMyProfile: 이미지가 변경되지 않으면 삭제하지 않는다")
    void updateMyProfile_sameImage_noDelete() {
        // given
        User user = User.builder()
                .nickname("oldNick")
                .profileImagePath("images/profiles/same.png")
                .leaderIntro("old leader")
                .memberIntro("old member")
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "images/profiles/same.png",  // 같은 이미지
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(5L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("images/profiles/same.png"))
                .willReturn("https://bucket.s3.amazonaws.com/images/profiles/same.png");

        // when
        userService.updateMyProfile(5L, request);

        // then
        // deleteImageIfChanged는 호출되지만, FileService 내부에서 같은 이미지면 실제 삭제는 안 함
        then(fileService).should().deleteImageIfChanged("images/profiles/same.png", "images/profiles/same.png");
    }

    @Test
    @DisplayName("updateMyProfile: 외부 URL로 변경 시 이전 이미지만 삭제한다")
    void updateMyProfile_toExternalUrl_deleteOldImage() {
        // given
        User user = User.builder()
                .nickname("oldNick")
                .profileImagePath("images/profiles/old.png")
                .leaderIntro("old leader")
                .memberIntro("old member")
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "https://k.kakaocdn.net/dn/profile/new.jpg",  // 외부 URL
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(5L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("https://k.kakaocdn.net/dn/profile/new.jpg"))
                .willReturn("https://k.kakaocdn.net/dn/profile/new.jpg");

        // when
        userService.updateMyProfile(5L, request);

        // then
        assertThat(user.getProfileImagePath()).isEqualTo("https://k.kakaocdn.net/dn/profile/new.jpg");
        // FileService가 외부 URL은 삭제하지 않음을 확인
        then(fileService).should().deleteImageIfChanged(
                "images/profiles/old.png",
                "https://k.kakaocdn.net/dn/profile/new.jpg"
        );
    }

    @Test
    @DisplayName("updateMyProfile: 외부 URL에서 우리 이미지로 변경 시 외부 URL은 삭제 시도하지 않는다")
    void updateMyProfile_fromExternalUrl_noDelete() {
        // given
        User user = User.builder()
                .nickname("oldNick")
                .profileImagePath("https://k.kakaocdn.net/dn/profile/old.jpg")
                .leaderIntro("old leader")
                .memberIntro("old member")
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "images/profiles/new.png",
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(5L)).willReturn(Optional.of(user));
        given(imageUrlResolver.toUrl("images/profiles/new.png"))
                .willReturn("https://bucket.s3.amazonaws.com/images/profiles/new.png");

        // when
        userService.updateMyProfile(5L, request);

        // then
        assertThat(user.getProfileImagePath()).isEqualTo("images/profiles/new.png");
        // FileService에서 외부 URL은 삭제 스킵
        then(fileService).should().deleteImageIfChanged(
                "https://k.kakaocdn.net/dn/profile/old.jpg",
                "images/profiles/new.png"
        );
    }

    @Test
    @DisplayName("updateMyProfile: 사용자가 없으면 예외가 발생한다")
    void updateMyProfile_notFound_throws() {
        // given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "newNick",
                "images/profiles/new.png",
                "new leader",
                "new member"
        );

        given(userRepository.findByIdAndDeletedAtIsNull(7L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateMyProfile(7L, request))
                .isInstanceOf(UserNotFoundException.class);

        then(fileService).should(never()).deleteImageIfChanged(any(), any());
    }
}