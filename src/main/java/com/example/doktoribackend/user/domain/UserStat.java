package com.example.doktoribackend.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStat {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "total_exp", nullable = false)
    private Long totalExp = 0L;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 1;

    @Column(name = "consecutive_days", nullable = false)
    private Integer consecutiveDays = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserStat(User user, Long totalExp, Integer currentLevel,
                    Integer consecutiveDays, LocalDate lastAttendanceDate) {
        this.user = user;
        this.totalExp = (totalExp != null) ? totalExp : 0L;
        this.currentLevel = (currentLevel != null) ? currentLevel : 1;
        this.consecutiveDays = (consecutiveDays != null) ? consecutiveDays : 0;
        this.lastAttendanceDate = lastAttendanceDate;
    }
}