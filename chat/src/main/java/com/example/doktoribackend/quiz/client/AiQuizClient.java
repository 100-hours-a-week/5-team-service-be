package com.example.doktoribackend.quiz.client;

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
 * 예외를 이 메서드 안에서 BusinessException 으로 변환하면 Resilience4j 의 recordExceptions 가
 * 원시 예외를 보지 못해 서킷이 열리지 않는다. 변환은 재시도가 소진된 뒤 fallback 에서만 수행한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiQuizClient {

    private final RestClient aiRestClient;

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiQuiz", fallbackMethod = "generateFallback")
    public AiQuizGenerateResponse generate(AiQuizGenerateRequest request) {
        AiQuizGenerateResponse body = aiRestClient.post()
                .uri("/quiz/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiQuizGenerateResponse.class);

        if (body == null) {
            log.warn("AI quiz generation returned null response");
            throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED);
        }

        return body;
    }

    AiQuizGenerateResponse generateFallback(AiQuizGenerateRequest request, Throwable throwable) {
        log.error("AI 퀴즈 생성 실패 - error={}: {}",
                throwable.getClass().getSimpleName(), throwable.getMessage());
        throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED);
    }
}
