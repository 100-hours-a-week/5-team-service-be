package com.example.doktoribackend.user.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import com.example.doktoribackend.user.domain.preference.UserPreference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.example.doktoribackend.common.constants.ValidationConstant.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NICKNAME_MAX_LENGTH)
    private String nickname;

    @Column(name = "profile_image_path", length = PROFILE_IMAGE_PATH_MAX_LENGTH)
    private String profileImagePath;

    @Column(name = "leader_intro", length = INTRO_MAX_LENGTH)
    private String leaderIntro;

    @Column(name = "member_intro", length = INTRO_MAX_LENGTH)
    private String memberIntro;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private UserPreference userPreference;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private UserAccount userAccount;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private UserStat userStat;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String nickname, String profileImagePath,
                String leaderIntro, String memberIntro,
                Boolean onboardingCompleted) {
        this.nickname = nickname;
        this.profileImagePath = profileImagePath;
        this.leaderIntro = leaderIntro;
        this.memberIntro = memberIntro;
        this.onboardingCompleted = onboardingCompleted != null
                && onboardingCompleted;
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public void updateProfileImage(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public void updateLeaderIntro(String leaderIntro) {
        this.leaderIntro = leaderIntro;
    }

    public void updateMemberIntro(String memberIntro) {
        this.memberIntro = memberIntro;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void linkPreference(UserPreference userPreference) {
        this.userPreference = userPreference;
    }

    public void linkAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public void linkStat(UserStat userStat) {
        this.userStat = userStat;
    }
}
