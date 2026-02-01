package com.example.doktoribackend.user.domain.preference;

import com.example.doktoribackend.reading.domain.ReadingGenre;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.domain.id.UserReadingGenreId;
import com.example.doktoribackend.user.domain.id.UserReadingPurposeId;
import com.example.doktoribackend.user.policy.ReadingPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserReadingRelationTest {

    @Test
    @DisplayName("UserReadingGenre.create: 사용자와 장르를 연결하며 복합키를 설정한다")
    void userReadingGenre_createBuildsCompositeKey() {
        User user = createUserWithId(10L);
        ReadingGenre readingGenre = createReadingGenreWithId();

        UserReadingGenre userReadingGenre = UserReadingGenre.create(user, readingGenre);

        assertThat(userReadingGenre.getUser()).isSameAs(user);
        assertThat(userReadingGenre.getReadingGenre()).isSameAs(readingGenre);
        assertThat(userReadingGenre.getId()).isEqualTo(new UserReadingGenreId(10L, 20L));
    }

    @Test
    @DisplayName("UserReadingPurpose.create: 사용자와 독서 목적을 연결하며 복합키를 설정한다")
    void userReadingPurpose_createBuildsCompositeKey() {
        User user = createUserWithId(5L);
        ReadingPurpose readingPurpose = createReadingPurposeWithId();

        UserReadingPurpose userReadingPurpose = UserReadingPurpose.create(user, readingPurpose);

        assertThat(userReadingPurpose.getUser()).isSameAs(user);
        assertThat(userReadingPurpose.getReadingPurpose()).isSameAs(readingPurpose);
        assertThat(userReadingPurpose.getId()).isEqualTo(new UserReadingPurposeId(5L, 7L));
    }

    private User createUserWithId(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private ReadingGenre createReadingGenreWithId() {
        ReadingGenre genre = mock(ReadingGenre.class);
        when(genre.getId()).thenReturn(20L);
        return genre;
    }

    private ReadingPurpose createReadingPurposeWithId() {
        ReadingPurpose purpose = mock(ReadingPurpose.class);
        when(purpose.getId()).thenReturn(7L);
        return purpose;
    }
}
