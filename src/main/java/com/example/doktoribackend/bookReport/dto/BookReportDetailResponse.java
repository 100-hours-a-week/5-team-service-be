package com.example.doktoribackend.bookReport.dto;

import java.time.LocalDate;

public record BookReportDetailResponse(
        BookInfo book,
        BookReportInfo bookReport
) {
    public record BookInfo(
            String title,
            String authors,
            String publisher,
            String thumbnailUrl,
            LocalDate publishedAt
    ) {}

    public record BookReportInfo(
            Long id,
            String status,
            String content,
            String rejectionReason
    ) {}
}
