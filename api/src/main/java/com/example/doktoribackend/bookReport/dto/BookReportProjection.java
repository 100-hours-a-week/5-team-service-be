package com.example.doktoribackend.bookReport.dto;

import com.example.doktoribackend.bookReport.domain.BookReportStatus;

public interface BookReportProjection {
    Long getId();
    BookReportStatus getStatus();
    String getContent();
    String getRejectionReason();
}
