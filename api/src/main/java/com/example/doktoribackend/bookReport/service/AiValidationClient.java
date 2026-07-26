package com.example.doktoribackend.bookReport.service;

import com.example.doktoribackend.bookReport.dto.AiValidationRequest;
import com.example.doktoribackend.bookReport.dto.AiValidationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 독후감 AI 검증 호출.
 *
 * <p>재시도와 서킷 브레이커는 Resilience4j 설정이 담당한다. 예외를 이 메서드 안에서
 * 삼키면 recordExceptions 가 원시 예외를 보지 못해 서킷이 열리지 않으므로, 변환은
 * 재시도가 모두 소진된 뒤 fallback 에서만 수행한다.
 *
 * <p>AiValidationService 에서 자기호출(self-invocation)로 부르면 Spring 프록시를
 * 우회해 애노테이션이 무시되므로 별도 빈으로 분리했다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiValidationClient {

    private final RestClient aiRestClient;

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiValidation", fallbackMethod = "validateFallback")
    public AiValidationResponse validate(Long bookReportId, AiValidationRequest request) {
        return aiRestClient.post()
                .uri("/book-reports/" + bookReportId + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiValidationResponse.class);
    }

    /**
     * 재시도 소진 또는 서킷 Open 시 호출된다. 독후감 검증은 비동기 후처리라
     * 예외를 던져도 받을 곳이 없으므로 null 을 반환해 호출자가 상태를 그대로 두게 한다.
     */
    @SuppressWarnings("unused")
    private AiValidationResponse validateFallback(Long bookReportId, AiValidationRequest request, Throwable throwable) {
        log.error("AI 독후감 검증 실패 - bookReportId={}, error={}: {}",
                bookReportId, throwable.getClass().getSimpleName(), throwable.getMessage());
        return null;
    }
}
