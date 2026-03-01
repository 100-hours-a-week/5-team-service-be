package com.example.doktoribackend.meeting.dto.validator;

import com.example.doktoribackend.meeting.dto.MeetingUpdateRequest;
import com.example.doktoribackend.meeting.dto.RoundRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MeetingUpdateRequestValidator
        implements ConstraintValidator<ValidMeetingUpdateRequest, MeetingUpdateRequest> {

    private static final String FIELD_ROUNDS = "rounds";
    private static final String FIELD_ROUND_COUNT = "roundCount";
    private static final String FIELD_DURATION_MINUTES = "durationMinutes";
    private static final String FIELD_RECRUITMENT_DEADLINE = "recruitmentDeadline";

    @Override
    public boolean isValid(MeetingUpdateRequest req, ConstraintValidatorContext ctx) {
        if (req == null) {
            return true;
        }

        ctx.disableDefaultConstraintViolation();
        boolean valid = true;

        if (!isValidRoundCount(req)) {
            addViolation(ctx, FIELD_ROUND_COUNT, "회차 수와 rounds 배열 크기가 일치해야 합니다");
            valid = false;
        }

        if (!isValidRoundNumbers(req)) {
            addViolation(ctx, FIELD_ROUNDS, "회차 번호는 1부터 회차 수까지 중복 없이 존재해야 합니다");
            valid = false;
        }

        if (!isValidDurationMinutes(req)) {
            addViolation(ctx, FIELD_DURATION_MINUTES, "진행 시간은 30분이어야 합니다");
            valid = false;
        }

        if (!isRoundDatesInOrder(req)) {
            addViolation(ctx, FIELD_ROUNDS, "회차 날짜는 순서대로 증가해야 합니다");
            valid = false;
        }

        if (!isRecruitmentDeadlineBeforeLastRound(req)) {
            addViolation(ctx, FIELD_RECRUITMENT_DEADLINE, "모집 마감일은 마지막 회차 날짜 이전이어야 합니다");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext ctx, String field, String message) {
        ctx.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }

    private boolean isValidRoundCount(MeetingUpdateRequest req) {
        if (req.roundCount() == null || req.rounds() == null) {
            return true;
        }
        return req.roundCount().equals(req.rounds().size());
    }

    private boolean isValidRoundNumbers(MeetingUpdateRequest req) {
        if (req.roundCount() == null || req.rounds() == null) {
            return true;
        }
        Set<Integer> numbers = new HashSet<>();
        for (RoundRequest round : req.rounds()) {
            if (round == null || round.roundNo() == null) {
                return false;
            }
            if (round.roundNo() < 1 || round.roundNo() > req.roundCount()) {
                return false;
            }
            if (!numbers.add(round.roundNo())) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidDurationMinutes(MeetingUpdateRequest req) {
        if (req.durationMinutes() == null) {
            return true;
        }
        return req.durationMinutes() == 30;
    }

    private boolean isRoundDatesInOrder(MeetingUpdateRequest req) {
        if (req.rounds() == null || req.rounds().size() < 2) {
            return true;
        }
        List<RoundRequest> sorted = req.rounds().stream()
                .filter(r -> r.roundNo() != null && r.date() != null)
                .sorted(Comparator.comparing(RoundRequest::roundNo))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (!sorted.get(i).date().isAfter(sorted.get(i - 1).date())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRecruitmentDeadlineBeforeLastRound(MeetingUpdateRequest req) {
        if (req.recruitmentDeadline() == null || req.rounds() == null || req.rounds().isEmpty()) {
            return true;
        }
        LocalDate lastRoundDate = req.rounds().stream()
                .map(RoundRequest::date)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (lastRoundDate == null) {
            return true;
        }
        return req.recruitmentDeadline().isBefore(lastRoundDate);
    }
}
