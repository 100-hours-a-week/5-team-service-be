package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.AiTopicRequest;
import com.example.doktoribackend.meeting.dto.AiTopicResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTopicRecommendationClient {

    private final RestTemplate restTemplate;

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    public AiTopicResponse requestTopicRecommendation(Long meetingRoundId, AiTopicRequest request) {
        String url = aiBaseUrl + "/meeting-rounds/" + meetingRoundId + "/discussion-topics/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<AiTopicRequest> httpRequest = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AiTopicResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    AiTopicResponse.class
            );

            AiTopicResponse body = response.getBody();
            if (body != null && body.isSuccess() && body.data() != null) {
                return body;
            }

            log.warn("AI topic recommendation returned invalid response for meetingRoundId: {}", meetingRoundId);
            throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);

        } catch (RestClientException ex) {
            log.error("AI topic recommendation failed for meetingRoundId: {}, error: {}",
                    meetingRoundId, ex.getMessage());
            throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);
        }
    }
}
