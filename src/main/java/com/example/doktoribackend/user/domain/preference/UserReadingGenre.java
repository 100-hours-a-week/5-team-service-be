package com.example.doktoribackend.user.domain.preference;

import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.id.UserReadingGenreId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_reading_genres")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserReadingGenre {

    @EmbeddedId
    private UserReadingGenreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("readingGenreId")
    @JoinColumn(name = "reading_genre_id")
    private ReadingGenre readingGenre;
}
