package com.example.doktoribackend.review.domain;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.meeting.domain.MeetingRound;

import java.time.LocalDateTime;
import java.util.Optional;

public class ReviewStatusResolver {

    private ReviewStatusResolver() {
    }

    public static UserReviewStatus resolveNotSubmitted(
            LocalDateTime now,
            MeetingRound round,
            Optional<BookReport> bookReportOpt
    ) {
        // 회차 종료 안됨
        if (now.isBefore(round.getEndAt())) {
            return UserReviewStatus.NOT_YET_WRITABLE;
        }

        // 독후감 미승인
        if (bookReportOpt.isEmpty() || bookReportOpt.get().getStatus() != BookReportStatus.APPROVED) {
            return UserReviewStatus.NOT_YET_WRITABLE;
        }

        // 24시간 경과
        if (now.isAfter(round.getEndAt().plusHours(24))) {
            return UserReviewStatus.DEADLINE_PASSED;
        }

        return UserReviewStatus.NOT_SUBMITTED;
    }
}
