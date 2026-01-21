package com.example.doktoribackend.user.domain.preference;

import com.example.doktoribackend.user.domain.Gender;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.policy.ReadingVolume;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserPreferenceTest {

    @Test
    @DisplayName("builder: 성별과 출생연도를 지정하지 않으면 기본값으로 초기화된다")
    void builder_setsDefaultValuesWhenNull() {
        UserPreference preference = UserPreference.builder()
                .user(createUser())
                .build();

        assertThat(preference.getGender()).isEqualTo(Gender.UNKNOWN);
        assertThat(preference.getBirthYear()).isZero();
        assertThat(preference.getReadingVolume()).isNull();
    }

    @Test
    @DisplayName("updateOAuthInfo: null이 아닌 값만 갱신한다")
    void updateOAuthInfo_updatesOnlyNonNullFields() {
        UserPreference preference = UserPreference.builder()
                .user(createUser())
                .gender(Gender.MALE)
                .birthYear(1990)
                .build();

        preference.updateOAuthInfo(Gender.FEMALE, 2000);
        assertThat(preference.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(preference.getBirthYear()).isEqualTo(2000);

        preference.updateOAuthInfo(null, null);
        assertThat(preference.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(preference.getBirthYear()).isEqualTo(2000);
    }

    @Test
    @DisplayName("updateOnboardingInfo: 독서량 정보가 제공될 때만 갱신한다")
    void updateOnboardingInfo_updatesReadingVolumeWhenProvided() {
        UserPreference preference = UserPreference.builder()
                .user(createUser())
                .build();

        ReadingVolume readingVolume = mock(ReadingVolume.class);

        preference.updateOnboardingInfo(readingVolume);
        assertThat(preference.getReadingVolume()).isSameAs(readingVolume);

        preference.updateOnboardingInfo(null);
        assertThat(preference.getReadingVolume()).isSameAs(readingVolume);
    }

    private User createUser() {
        return User.builder()
                .nickname("user")
                .profileImagePath("/images/profile.png")
                .leaderIntro("leader")
                .memberIntro("member")
                .build();
    }
}
