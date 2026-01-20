package com.example.doktoribackend.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_volume_id")
    private ReadingVolume readingVolume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender = Gender.UNKNOWN;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserPreference(User user,
                          ReadingVolume readingVolume,
                          Gender gender,
                          Integer birthYear) {
        this.user = user;
        this.readingVolume = readingVolume;
        this.gender = (gender != null) ? gender : Gender.UNKNOWN;
        this.birthYear = birthYear;
    }

    public void updateOAuthInfo(Gender gender, Integer birthYear) {
        if (gender != null) {
            this.gender = gender;
        }
        if (birthYear != null) {
            this.birthYear = birthYear;
        }
    }

    public void updateOnboardingInfo(ReadingVolume readingVolume) {
        if (readingVolume != null) {
            this.readingVolume = readingVolume;
        }
    }
}