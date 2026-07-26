package com.example.doktoribackend.summary.client;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 예외 변환은 재시도가 소진된 뒤 fallback 에서만 수행한다. 자세한 이유는 AiQuizClient 참고.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiSummaryClient {

    private final RestClient aiRestClient;

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiSummary", fallbackMethod = "requestSummaryFallback")
    public AiSummaryResponse requestSummary(Long roomId, AiSummaryRequest request) {
        AiSummaryResponse body = aiRestClient.post()
                .uri("/chat-rooms/" + roomId + "/discussion-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiSummaryResponse.class);

        if (body == null) {
            log.warn("AI summary generation returned null response - roomId={}", roomId);
            throw new BusinessException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
        }

        return body;
    }

    AiSummaryResponse requestSummaryFallback(Long roomId, AiSummaryRequest request, Throwable throwable) {
        log.error("AI 요약 생성 실패 - roomId={}, error={}: {}",
                roomId, throwable.getClass().getSimpleName(), throwable.getMessage());
        throw new BusinessException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
    }
}
