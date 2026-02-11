package com.example.doktoribackend.meeting.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = MeetingCreateRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeetingCreateRequest {
    String message() default "모임 생성 요청이 유효하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
