package com.example.doktoribackend.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ImageUrlValidator implements ConstraintValidator<ValidImageUrl, String> {

    private static final Pattern KAKAO_CDN_PATTERN = Pattern.compile(
            "^https://k\\.kakaocdn\\.net/.+"
    );

    private static final Pattern S3_KEY_PATTERN = Pattern.compile(
            "^images/(profiles|meetings)/[a-zA-Z0-9._-]+\\.(jpg|jpeg|png|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return isKakaoCdnUrl(value) || isS3Key(value);
    }

    private boolean isKakaoCdnUrl(String url) {
        return KAKAO_CDN_PATTERN.matcher(url).matches();
    }

    private boolean isS3Key(String key) {
        return S3_KEY_PATTERN.matcher(key).matches();
    }
}
