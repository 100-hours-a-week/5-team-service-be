package com.example.doktoribackend.user.domain.preference;

import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.id.UserReadingPurposeId;
import com.example.doktoribackend.user.policy.ReadingPurpose;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_reading_purposes")
@Getter
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

    public static UserReadingPurpose create(User user, ReadingPurpose readingPurpose) {
        UserReadingPurpose userReadingPurpose = new UserReadingPurpose();
        userReadingPurpose.user = user;
        userReadingPurpose.readingPurpose = readingPurpose;
        userReadingPurpose.id = new UserReadingPurposeId(
                user.getId(),
                readingPurpose.getId()
        );
        return userReadingPurpose;
    }
}
