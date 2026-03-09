package com.example.doktoribackend.review.dto.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class RatingStepValidator implements ConstraintValidator<ValidRatingStep, BigDecimal> {

    private static final BigDecimal STEP = new BigDecimal("0.5");

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.remainder(STEP).compareTo(BigDecimal.ZERO) == 0;
    }
}
