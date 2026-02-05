package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.dto.AiValidationRequest;
import com.example.doktoribackend.bookReport.dto.AiValidationResponse;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiValidationService {

    private final RestTemplate restTemplate;
    private final BookReportRepository bookReportRepository;
    private final PlatformTransactionManager transactionManager;
    private final NotificationService notificationService;

    @Value("${ai.base-url}")
    private String aiValidationBaseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Async("aiValidationExecutor")
    public void validate(Long bookReportId, String bookTitle, String content) {
        AiValidationRequest request = new AiValidationRequest(bookTitle, content);

        String url = aiValidationBaseUrl + "/book-reports/" + bookReportId + "/validate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<AiValidationRequest> httpRequest = new HttpEntity<>(request, headers);

        AiValidationResponse response = executeWithRetry(url, httpRequest, bookReportId);

        if (response != null) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                updateBookReportStatus(bookReportId, response);
                return null;
            });
        }
    }

    private AiValidationResponse executeWithRetry(String url, HttpEntity<AiValidationRequest> request, Long bookReportId) {
        int attempt = 0;
        while (attempt < MAX_RETRY) {
            try {
                ResponseEntity<AiValidationResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        AiValidationResponse.class
                );
                return response.getBody();
            } catch (RestClientException ex) {
                attempt++;
                log.warn("Retrying AI validation for bookReportId: {}, attempt: {}, error: {}",
                        bookReportId, attempt, ex.getMessage());

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("AI validation retry interrupted for bookReportId: {}", bookReportId);
                        return null;
                    }
                }
            }
        }

        log.error("AI validation failed for bookReportId: {} after {} retries", bookReportId, MAX_RETRY);
        return null;
    }

    private void updateBookReportStatus(Long bookReportId, AiValidationResponse response) {
        BookReport bookReport = bookReportRepository.findById(bookReportId)
                .orElse(null);

        if (bookReport == null) {
            return;
        }

        if ("SUBMITTED".equals(response.status())) {
            bookReport.approve();
        } else if ("REJECTED".equals(response.status())) {
            bookReport.reject(response.rejectionReason());
        }

        bookReportRepository.save(bookReport);

        Long userId = bookReport.getUser().getId();
        Long meetingId = bookReport.getMeetingRound().getMeeting().getId();

        try {
            String meetingTitle = bookReport.getMeetingRound().getMeeting().getTitle();
            notificationService.createAndSend(
                    userId,
                    NotificationTypeCode.BOOK_REPORT_CHECKED,
                    Map.of("meetingId", String.valueOf(meetingId),
                            "meetingTitle", meetingTitle)
            );
        } catch (Exception e) {
            log.error("Failed to send notification");
        }
    }
}