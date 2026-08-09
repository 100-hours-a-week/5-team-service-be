package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.dto.AiValidationResponse;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.meeting.domain.Meeting;
import com.example.doktoribackend.meeting.domain.MeetingRound;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 재시도·서킷 브레이커는 {@link AiValidationClient} 의 Resilience4j 설정이 담당하므로
 * 이 테스트는 AI 응답을 받은 뒤의 상태 전이만 검증한다.
 * 재시도 동작 자체는 common 모듈의 ResilienceConfigTest 에서 설정 단위로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiValidationServiceTest {

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AiValidationClient aiValidationClient;

    @InjectMocks
    private AiValidationService aiValidationService;

    @Nested
    @DisplayName("AI 응답 상태별 처리")
    class StatusHandlingTests {

        @Test
        @DisplayName("SUBMITTED 응답 → 독후감 승인, save, 알림 발송")
        void submitted_approvesAndSavesAndNotifies() {
            Long bookReportId = 1L;
            BookReport bookReport = createMockBookReport(bookReportId, 10L, 100L, "테스트 모임");

            given(aiValidationClient.validate(anyLong(), any()))
                    .willReturn(new AiValidationResponse("SUBMITTED", null));
            given(bookReportRepository.findById(bookReportId))
                    .willReturn(Optional.of(bookReport));

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReport).approve();
            verify(bookReportRepository).save(bookReport);
            verify(notificationService).createAndSend(
                    eq(10L),
                    eq(NotificationTypeCode.BOOK_REPORT_CHECKED),
                    eq(Map.of("meetingId", "100", "meetingTitle", "테스트 모임"))
            );
        }

        @Test
        @DisplayName("REJECTED 응답 → 거절 사유와 함께 reject, save, 알림 발송")
        void rejected_rejectsWithReasonAndSavesAndNotifies() {
            Long bookReportId = 1L;
            BookReport bookReport = createMockBookReport(bookReportId, 10L, 100L, "테스트 모임");
            String rejectionReason = "책과 관련 없는 내용입니다.";

            given(aiValidationClient.validate(anyLong(), any()))
                    .willReturn(new AiValidationResponse("REJECTED", rejectionReason));
            given(bookReportRepository.findById(bookReportId))
                    .willReturn(Optional.of(bookReport));

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReport).reject(rejectionReason);
            verify(bookReportRepository).save(bookReport);
            verify(notificationService).createAndSend(
                    eq(10L),
                    eq(NotificationTypeCode.BOOK_REPORT_CHECKED),
                    anyMap()
            );
        }

        @Test
        @DisplayName("알 수 없는 status 응답 → approve/reject/save/알림 모두 실행하지 않는다")
        void unknownStatus_doesNotSaveOrNotify() {
            Long bookReportId = 1L;
            BookReport bookReport = createMockBookReport(bookReportId, 10L, 100L, "테스트 모임");

            given(aiValidationClient.validate(anyLong(), any()))
                    .willReturn(new AiValidationResponse("UNKNOWN_STATUS", null));
            given(bookReportRepository.findById(bookReportId))
                    .willReturn(Optional.of(bookReport));

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReport, never()).approve();
            verify(bookReport, never()).reject(any());
            verify(bookReportRepository, never()).save(any());
            verify(notificationService, never()).createAndSend(any(), any(), anyMap());
        }

        @Test
        @DisplayName("독후감이 DB에 없으면 save와 알림을 실행하지 않는다")
        void bookReportNotFound_doesNotSaveOrNotify() {
            Long bookReportId = 999L;

            given(aiValidationClient.validate(anyLong(), any()))
                    .willReturn(new AiValidationResponse("SUBMITTED", null));
            given(bookReportRepository.findById(bookReportId))
                    .willReturn(Optional.empty());

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReportRepository, never()).save(any());
            verify(notificationService, never()).createAndSend(any(), any(), anyMap());
        }

        @Test
        @DisplayName("알림 발송 실패해도 예외가 전파되지 않는다")
        void notificationFails_doesNotPropagateException() {
            Long bookReportId = 1L;
            BookReport bookReport = createMockBookReport(bookReportId, 10L, 100L, "테스트 모임");

            given(aiValidationClient.validate(anyLong(), any()))
                    .willReturn(new AiValidationResponse("SUBMITTED", null));
            given(bookReportRepository.findById(bookReportId))
                    .willReturn(Optional.of(bookReport));
            willThrow(new RuntimeException("알림 서버 오류"))
                    .given(notificationService).createAndSend(any(), any(), anyMap());

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReport).approve();
            verify(bookReportRepository).save(bookReport);
        }
    }

    @Nested
    @DisplayName("AI 호출 실패 처리")
    class FailureTests {

        @Test
        @DisplayName("재시도까지 모두 실패해 클라이언트가 null을 반환하면 독후감 상태를 변경하지 않는다")
        void clientReturnsNull_doesNotUpdateStatus() {
            Long bookReportId = 1L;

            given(aiValidationClient.validate(anyLong(), any())).willReturn(null);

            aiValidationService.validate(bookReportId, "책 제목", "독후감 내용");

            verify(bookReportRepository, never()).save(any());
            verify(notificationService, never()).createAndSend(any(), any(), anyMap());
        }
    }

    private BookReport createMockBookReport(Long id, Long userId, Long meetingId, String meetingTitle) {
        Meeting meeting = mock(Meeting.class);
        given(meeting.getId()).willReturn(meetingId);
        given(meeting.getTitle()).willReturn(meetingTitle);

        MeetingRound meetingRound = mock(MeetingRound.class);
        given(meetingRound.getMeeting()).willReturn(meeting);

        User user = mock(User.class);
        given(user.getId()).willReturn(userId);

        BookReport bookReport = mock(BookReport.class);
        given(bookReport.getId()).willReturn(id);
        given(bookReport.getUser()).willReturn(user);
        given(bookReport.getMeetingRound()).willReturn(meetingRound);

        return bookReport;
    }
}
