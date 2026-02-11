package com.example.doktoribackend.user.domain;

import com.example.doktoribackend.auth.dto.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    @DisplayName("builder: 사용자와 OAuth 정보가 설정된다")
    void builder_setsUserAndOAuthFields() {
        User user = User.builder()
                .nickname("user")
                .profileImagePath("/images/profile.png")
                .leaderIntro("leader")
                .memberIntro("member")
                .build();

        UserAccount userAccount = UserAccount.builder()
                .user(user)
                .provider(OAuthProvider.KAKAO)
                .providerId("123456")
                .build();

        assertThat(userAccount.getUser()).isSameAs(user);
        assertThat(userAccount.getProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(userAccount.getProviderId()).isEqualTo("123456");
    }
}
