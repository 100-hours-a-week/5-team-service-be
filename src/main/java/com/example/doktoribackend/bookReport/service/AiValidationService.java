package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.domain.BookReport;
import com.example.doktoribackend.bookReport.domain.BookReportStatus;
import com.example.doktoribackend.bookReport.dto.AiValidationRequest;
import com.example.doktoribackend.bookReport.dto.AiValidationResponse;
import com.example.doktoribackend.bookReport.repository.BookReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiValidationService {

    private final WebClient webClient;
    private final BookReportRepository bookReportRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${ai.base-url}")
    private String aiValidationBaseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_RETRY = 3;

    public void validate(Long bookReportId, String bookTitle, String content) {
        AiValidationRequest request = new AiValidationRequest(
                bookTitle,
                content
        );

        webClient.post()
                .uri(aiValidationBaseUrl + "/api/book-reports/{id}/validate", bookReportId)
                .header("x-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiValidationResponse.class)
                .timeout(TIMEOUT)
                .retryWhen(Retry.backoff(MAX_RETRY, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(signal -> log.warn("Retrying AI validation for bookReportId: {}, attempt: {}",
                                bookReportId, signal.totalRetries() + 1)))
                .doOnSuccess(response -> {
                    if (response != null) {
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.execute(status -> {
                            updateBookReportStatus(bookReportId, response);
                            return null;
                        });
                    }
                })
                .doOnError(e -> log.error("AI validation failed for bookReportId: {} after {} retries. Error: {}",
                        bookReportId, MAX_RETRY, e.getMessage()))
                .subscribe();
    }

    private void updateBookReportStatus(Long bookReportId, AiValidationResponse response) {
        BookReport bookReport = bookReportRepository.findById(bookReportId)
                .orElse(null);

        if (bookReport == null) {
            log.warn("BookReport not found for id: {}", bookReportId);
            return;
        }

        if (BookReportStatus.APPROVED.name().equals(response.status())) {
            bookReport.approve();
        } else if (BookReportStatus.REJECTED.name().equals(response.status())) {
            bookReport.reject(response.rejectionReason());
        }

        bookReportRepository.save(bookReport);
        log.info("BookReport {} validated: {}", bookReportId, response.status());
    }
}