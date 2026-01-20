package com.example.doktoribackend.user.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_path")
    private String profileImagePath;

    @Column(name = "leader_intro")
    private String leaderIntro;

    @Column(name = "member_intro")
    private String memberIntro;

    @Column(name = "is_onboarding_completed", nullable = false)
    private boolean isOnboardingCompleted = false;

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

    @Builder
    public User(String nickname, String profileImagePath,
                String leaderIntro, String memberIntro,
                Boolean isOnboardingCompleted) {
        this.nickname = nickname;
        this.profileImagePath = profileImagePath;
        this.leaderIntro = leaderIntro;
        this.memberIntro = memberIntro;
        this.isOnboardingCompleted = isOnboardingCompleted != null
                ? isOnboardingCompleted : false;
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
        this.isOnboardingCompleted = true;
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
