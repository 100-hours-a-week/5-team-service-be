package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.AiTopicRequest;
import com.example.doktoribackend.meeting.dto.AiTopicResponse;
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
public class AiTopicRecommendationClient {

    private final RestClient aiRestClient;

    @CircuitBreaker(name = "aiServer")
    @Retry(name = "aiTopic", fallbackMethod = "requestTopicRecommendationFallback")
    public AiTopicResponse requestTopicRecommendation(Long meetingRoundId, AiTopicRequest request) {
        AiTopicResponse body = aiRestClient.post()
                .uri("/meeting-rounds/" + meetingRoundId + "/discussion-topics/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiTopicResponse.class);

        if (body == null || !body.isSuccess() || body.data() == null) {
            log.warn("AI topic recommendation returned invalid response for meetingRoundId: {}", meetingRoundId);
            throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);
        }

        return body;
    }

    AiTopicResponse requestTopicRecommendationFallback(Long meetingRoundId, AiTopicRequest request,
                                                       Throwable throwable) {
        log.error("AI 주제 추천 실패 - meetingRoundId={}, error={}: {}",
                meetingRoundId, throwable.getClass().getSimpleName(), throwable.getMessage());
        throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);
    }
}
