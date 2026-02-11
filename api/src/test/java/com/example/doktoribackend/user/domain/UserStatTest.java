package com.example.doktoribackend.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatTest {

    @Test
    @DisplayName("builder: 값이 주어지지 않으면 기본값으로 설정된다")
    void builder_appliesDefaultValuesWhenNull() {
        UserStat userStat = UserStat.builder()
                .user(createUser())
                .build();

        assertThat(userStat.getTotalExp()).isZero();
        assertThat(userStat.getCurrentLevel()).isEqualTo(1);
        assertThat(userStat.getConsecutiveDays()).isZero();
        assertThat(userStat.getLastAttendanceDate()).isNull();
    }

    @Test
    @DisplayName("builder: 전달한 값으로 필드가 설정된다")
    void builder_setsProvidedValues() {
        LocalDate attendanceDate = LocalDate.of(2024, 12, 25);

        UserStat userStat = UserStat.builder()
                .user(createUser())
                .totalExp(150L)
                .currentLevel(5)
                .consecutiveDays(10)
                .lastAttendanceDate(attendanceDate)
                .build();

        assertThat(userStat.getTotalExp()).isEqualTo(150L);
        assertThat(userStat.getCurrentLevel()).isEqualTo(5);
        assertThat(userStat.getConsecutiveDays()).isEqualTo(10);
        assertThat(userStat.getLastAttendanceDate()).isEqualTo(attendanceDate);
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
