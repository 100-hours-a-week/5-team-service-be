package com.example.doktoribackend.bookReport.domain;

public enum UserBookReportStatus {
    NOT_YET_WRITABLE,
    NOT_SUBMITTED,
    DEADLINE_PASSED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
