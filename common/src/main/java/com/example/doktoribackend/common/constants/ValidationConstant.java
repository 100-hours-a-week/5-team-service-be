package com.example.doktoribackend.common.constants;

public final class ValidationConstant {

    private ValidationConstant() {}

    public static final int NICKNAME_MAX_LENGTH = 20;
    public static final int INTRO_MAX_LENGTH = 300;
    public static final int PROFILE_IMAGE_PATH_MAX_LENGTH = 512;

    public static final long MAX_FILE_SIZE = 5_242_880L;

    // ChatRoom
    public static final int TOPIC_MIN_LENGTH = 2;
    public static final int TOPIC_MAX_LENGTH = 50;
    public static final int DESCRIPTION_MIN_LENGTH = 2;
    public static final int DESCRIPTION_MAX_LENGTH = 50;
    public static final String TEXT_PATTERN = "^[a-zA-Z0-9가-힣\\s]+$";

    // Quiz
    public static final int QUESTION_MIN_LENGTH = 2;
    public static final int QUESTION_MAX_LENGTH = 50;
    public static final int CHOICE_COUNT = 4;
    public static final int CHOICE_NUMBER_MIN = 1;
    public static final int CHOICE_NUMBER_MAX = 4;
    public static final int CHOICE_TEXT_MIN_LENGTH = 2;
    public static final int CHOICE_TEXT_MAX_LENGTH = 100;
}

