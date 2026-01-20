package com.example.doktoribackend.user.domain;

public enum Gender {
    FEMALE,
    MALE,
    UNKNOWN;

    public static Gender fromKakaoValue(String kakaoGender) {
        if (kakaoGender == null || kakaoGender.isBlank()) {
            return UNKNOWN;
        }

        String normalized = kakaoGender.trim().toLowerCase();

        return switch (normalized) {
            case "male" -> MALE;
            case "female" -> FEMALE;
            default -> UNKNOWN;
        };
    }
}
