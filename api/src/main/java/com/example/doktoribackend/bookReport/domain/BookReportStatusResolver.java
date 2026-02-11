package com.example.doktoribackend.bookReport.domain;

import com.example.doktoribackend.meeting.domain.MeetingRound;

import java.time.LocalDateTime;

public class BookReportStatusResolver {

    private BookReportStatusResolver() {
    }

    public static UserBookReportStatus resolveNotSubmitted(
            LocalDateTime now,
            MeetingRound meetingRound,
            MeetingRound prevRound
    ) {
        LocalDateTime deadline = meetingRound.getStartAt().minusHours(24);

        if (now.isAfter(deadline)) {
            return UserBookReportStatus.DEADLINE_PASSED;
        }

        int roundNo = meetingRound.getRoundNo();

        if (roundNo == 1) {
            return UserBookReportStatus.NOT_SUBMITTED;
        }

        if (prevRound == null) {
            return UserBookReportStatus.NOT_YET_WRITABLE;
        }

        LocalDateTime writableFrom = prevRound.getEndAt();

        if (now.isBefore(writableFrom)) {
            return UserBookReportStatus.NOT_YET_WRITABLE;
        }

        return UserBookReportStatus.NOT_SUBMITTED;
    }


    public static UserBookReportStatus fromBookReportStatus(BookReportStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> UserBookReportStatus.PENDING_REVIEW;
            case APPROVED -> UserBookReportStatus.APPROVED;
            case REJECTED -> UserBookReportStatus.REJECTED;
        };
    }
}
