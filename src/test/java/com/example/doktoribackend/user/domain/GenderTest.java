package com.example.doktoribackend.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GenderTest {

    @ParameterizedTest
    @CsvSource({
            "male, MALE",
            "Male, MALE",
            "' MALE ', MALE",
            "female, FEMALE",
            "Female, FEMALE",
            "' FEMALE ', FEMALE",
            "'\tMALE\n', MALE"
    })
    @DisplayName("fromKakaoValue: male/female는 대소문자 및 공백을 무시하고 매핑된다")
    void fromKakaoValue_mapsMaleAndFemaleIgnoringCase(String kakaoValue, Gender expected) {
        assertThat(Gender.fromKakaoValue(kakaoValue)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "unknown", "etc"})
    @DisplayName("fromKakaoValue: null/공백/알 수 없는 값은 UNKNOWN을 반환한다")
    void fromKakaoValue_returnsUnknownForInvalidValues(String kakaoValue) {
        assertThat(Gender.fromKakaoValue(kakaoValue)).isEqualTo(Gender.UNKNOWN);
    }
}
