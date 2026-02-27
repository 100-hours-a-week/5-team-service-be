package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookReportSchedulerServiceTest {

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookReportSchedulerService schedulerService;

    @Nested
    @DisplayName("failStalePendingReports")
    class FailStalePendingReportsTests {

        @Test
        @DisplayName("타임아웃된 PENDING_REVIEW가 없으면 bulk update와 알림을 실행하지 않는다")
        void noStaleReports_doesNothing() {
            given(bookReportRepository.findStalePendingReportProjections(any()))
                    .willReturn(Collections.emptyList());

            schedulerService.failStalePendingReports();

            verify(bookReportRepository, never()).bulkRejectStalePendingReports(any(), any(), any(), any());
            verify(notificationService, never()).createAndSend(any(), any(), anyMap());
        }

        @Test
        @DisplayName("타임아웃된 PENDING_REVIEW가 있으면 bulk update 1회 실행 후 알림 발송")
        void staleReportsExist_bulkUpdateAndNotify() {
            BookReportRepository.StaleReportProjection projection =
                    createMockProjection(1L, 10L, 100L, "테스트 모임");
            given(bookReportRepository.findStalePendingReportProjections(any()))
                    .willReturn(List.of(projection));
            given(bookReportRepository.bulkRejectStalePendingReports(any(), any(), any(), any()))
                    .willReturn(1);

            schedulerService.failStalePendingReports();

            verify(bookReportRepository, times(1)).bulkRejectStalePendingReports(
                    any(),
                    eq("AI 검증 처리 중 오류가 발생했습니다. 재제출해 주세요."),
                    any(),
                    eq(BookReportStatus.REJECTED)
            );
            verify(notificationService, times(1)).createAndSend(
                    eq(10L),
                    eq(NotificationTypeCode.BOOK_REPORT_CHECKED),
                    eq(Map.of("meetingId", "100", "meetingTitle", "테스트 모임"))
            );
        }

        @Test
        @DisplayName("여러 건이 있으면 bulk update는 1번, 알림은 건수만큼 발송된다")
        void multipleStaleReports_singleBulkUpdate_multipleNotifications() {
            List<BookReportRepository.StaleReportProjection> projections = List.of(
                    createMockProjection(1L, 10L, 100L, "모임A"),
                    createMockProjection(2L, 20L, 100L, "모임A"),
                    createMockProjection(3L, 30L, 200L, "모임B")
            );
            given(bookReportRepository.findStalePendingReportProjections(any()))
                    .willReturn(projections);
            given(bookReportRepository.bulkRejectStalePendingReports(any(), any(), any(), any()))
                    .willReturn(3);

            schedulerService.failStalePendingReports();

            verify(bookReportRepository, times(1)).bulkRejectStalePendingReports(any(), any(), any(), any());
            verify(notificationService, times(3)).createAndSend(any(), eq(NotificationTypeCode.BOOK_REPORT_CHECKED), anyMap());
        }

        @Test
        @DisplayName("일부 알림 발송이 실패해도 나머지 알림은 계속 발송된다")
        void notificationFails_continuesForRemainingReports() {
            BookReportRepository.StaleReportProjection p1 = createMockProjection(1L, 10L, 100L, "모임A");
            // p1은 알림 실패 시 catch 블록에서 getBookReportId() 호출됨
            given(p1.getBookReportId()).willReturn(1L);
            BookReportRepository.StaleReportProjection p2 = createMockProjection(2L, 20L, 200L, "모임B");
            BookReportRepository.StaleReportProjection p3 = createMockProjection(3L, 30L, 300L, "모임C");

            given(bookReportRepository.findStalePendingReportProjections(any()))
                    .willReturn(List.of(p1, p2, p3));
            given(bookReportRepository.bulkRejectStalePendingReports(any(), any(), any(), any()))
                    .willReturn(3);
            willThrow(new RuntimeException("알림 서버 오류"))
                    .given(notificationService).createAndSend(eq(10L), any(), anyMap());

            schedulerService.failStalePendingReports();

            // 실패한 1건 포함 3번 모두 시도
            verify(notificationService, times(3)).createAndSend(any(), any(), anyMap());
            // bulk update는 알림 실패와 무관하게 완료됨
            verify(bookReportRepository, times(1)).bulkRejectStalePendingReports(any(), any(), any(), any());
        }
    }

    private BookReportRepository.StaleReportProjection createMockProjection(
            Long bookReportId, Long userId, Long meetingId, String meetingTitle) {
        BookReportRepository.StaleReportProjection projection =
                mock(BookReportRepository.StaleReportProjection.class);
        // getBookReportId()는 catch 블록에서만 호출되므로 여기선 stub하지 않음
        given(projection.getUserId()).willReturn(userId);
        given(projection.getMeetingId()).willReturn(meetingId);
        given(projection.getMeetingTitle()).willReturn(meetingTitle);
        return projection;
    }
}
