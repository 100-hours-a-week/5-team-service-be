package com.example.doktoribackend.user.domain.preference;

import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.id.UserReadingPurposeId;
import com.example.doktoribackend.user.policy.ReadingPurpose;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_reading_purposes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserReadingPurpose {

    @EmbeddedId
    private UserReadingPurposeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("readingPurposeId")
    @JoinColumn(name = "reading_purpose_id")
    private ReadingPurpose readingPurpose;
}
