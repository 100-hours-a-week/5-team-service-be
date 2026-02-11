package com.example.doktoribackend.user.domain;

import com.example.doktoribackend.user.domain.preference.UserPreference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("builder: 온보딩 여부를 지정하지 않으면 false로 초기화된다")
    void builder_initializesOnboardingFlagToFalse() {
        User user = User.builder()
                .nickname("nickname")
                .profileImagePath("path")
                .leaderIntro("leader")
                .memberIntro("member")
                .build();

        assertThat(user.isOnboardingCompleted()).isFalse();
    }

    @Test
    @DisplayName("updateNickname: 공백이 아닌 값으로 변경된다")
    void updateNickname_updatesWhenValueIsNotBlank() {
        User user = createUser("original");

        user.updateNickname("newNickname");

        assertThat(user.getNickname()).isEqualTo("newNickname");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("updateNickname: null이나 공백이면 기존 값이 유지된다")
    void updateNickname_ignoresNullOrBlank(String nickname) {
        User user = createUser("keepsNickname");

        user.updateNickname(nickname);

        assertThat(user.getNickname()).isEqualTo("keepsNickname");
    }

    @Test
    @DisplayName("updateProfileImage: 새 경로로 변경한다")
    void updateProfileImage_updatesPath() {
        User user = createUser("user");

        user.updateProfileImage("/images/new.png");

        assertThat(user.getProfileImagePath()).isEqualTo("/images/new.png");
    }

    @Test
    @DisplayName("updateLeaderIntro: 리더 소개가 새 값으로 변경된다")
    void updateLeaderIntro_updatesIntro() {
        User user = createUser("user");

        user.updateLeaderIntro("새로운 리더 소개");

        assertThat(user.getLeaderIntro()).isEqualTo("새로운 리더 소개");
    }

    @Test
    @DisplayName("updateMemberIntro: 멤버 소개가 새 값으로 변경된다")
    void updateMemberIntro_updatesIntro() {
        User user = createUser("user");

        user.updateMemberIntro("새로운 멤버 소개");

        assertThat(user.getMemberIntro()).isEqualTo("새로운 멤버 소개");
    }

    @Test
    @DisplayName("완료 처리 시 온보딩 완료 플래그가 true가 된다")
    void completeOnboarding_setsFlagToTrue() {
        User user = createUser("user");

        user.completeOnboarding();

        assertThat(user.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("softDelete: 삭제 시각이 설정되고 삭제 상태를 반환한다")
    void softDelete_marksUserAsDeleted() {
        User user = createUser("user");

        assertThat(user.isDeleted()).isFalse();

        user.softDelete();

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("linkPreference: 선호 정보가 연결된다")
    void linkPreference_setsReference() {
        User user = createUser("user");
        UserPreference preference = UserPreference.builder()
                .user(user)
                .build();

        user.linkPreference(preference);

        assertThat(user.getUserPreference()).isSameAs(preference);
    }

    @Test
    @DisplayName("linkAccount: 계정 정보가 연결된다")
    void linkAccount_setsReference() {
        User user = createUser("user");
        UserAccount account = UserAccount.builder()
                .user(user)
                .provider(com.example.doktoribackend.auth.dto.OAuthProvider.KAKAO)
                .providerId("provider-id")
                .build();

        user.linkAccount(account);

        assertThat(user.getUserAccount()).isSameAs(account);
    }

    @Test
    @DisplayName("linkStat: 통계 정보가 연결된다")
    void linkStat_setsReference() {
        User user = createUser("user");
        UserStat stat = UserStat.builder()
                .user(user)
                .build();

        user.linkStat(stat);

        assertThat(user.getUserStat()).isSameAs(stat);
    }

    private User createUser(String nickname) {
        return User.builder()
                .nickname(nickname)
                .profileImagePath("/images/original.png")
                .leaderIntro("leader")
                .memberIntro("member")
                .build();
    }
}
