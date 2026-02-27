package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.AiTopicRequest;
import com.example.doktoribackend.meeting.dto.AiTopicResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTopicRecommendationClient {

    private final RestClient aiRestClient;

    public AiTopicResponse requestTopicRecommendation(Long meetingRoundId, AiTopicRequest request) {
        String uri = "/meeting-rounds/" + meetingRoundId + "/discussion-topics/generate";

        try {
            AiTopicResponse body = aiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("AI topic recommendation failed: status={}", res.getStatusCode());
                        throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);
                    })
                    .body(AiTopicResponse.class);

            if (body != null && body.isSuccess() && body.data() != null) {
                return body;
            }

            log.warn("AI topic recommendation returned invalid response for meetingRoundId: {}", meetingRoundId);
            throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI topic recommendation failed for meetingRoundId: {}, error: {}",
                    meetingRoundId, ex.getMessage());
            throw new BusinessException(ErrorCode.AI_TOPIC_RECOMMENDATION_FAILED);
        }
    }
}
