package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReportSchedulerService {

    private static final int PENDING_TIMEOUT_MINUTES = 10;
    private static final String SYSTEM_REJECTION_REASON = "AI 검증 처리 중 오류가 발생했습니다. 재제출해 주세요.";

    private final BookReportRepository bookReportRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void failStalePendingReports() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);

        List<BookReportRepository.StaleReportProjection> staleReports =
                bookReportRepository.findStalePendingReportProjections(threshold);

        if (staleReports.isEmpty()) {
            return;
        }

        log.warn("[Scheduler] PENDING_REVIEW 타임아웃 처리 시작 - 대상: {}건, 기준시각: {}", staleReports.size(), threshold);

        int updated = bookReportRepository.bulkRejectStalePendingReports(
                threshold,
                SYSTEM_REJECTION_REASON,
                LocalDateTime.now(),
                BookReportStatus.REJECTED
        );

        for (BookReportRepository.StaleReportProjection report : staleReports) {
            try {
                notificationService.createAndSend(
                        report.getUserId(),
                        NotificationTypeCode.BOOK_REPORT_CHECKED,
                        Map.of("meetingId", String.valueOf(report.getMeetingId()),
                                "meetingTitle", report.getMeetingTitle())
                );
            } catch (Exception e) {
                log.error("[Scheduler] 알림 발송 실패 - bookReportId: {}", report.getBookReportId(), e);
            }
        }

        log.warn("[Scheduler] PENDING_REVIEW 타임아웃 처리 완료 - {}건 REJECTED 처리됨", updated);
    }
}
