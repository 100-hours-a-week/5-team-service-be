package com.example.doktoribackend.summary.client;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiSummaryClient {

    private final RestClient aiRestClient;

    public AiSummaryResponse requestSummary(Long roomId, AiSummaryRequest request) {
        try {
            AiSummaryResponse body = aiRestClient.post()
                    .uri("/chat-rooms/" + roomId + "/discussion-summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.warn("AI summary generation failed: status={}, body={}", res.getStatusCode(), errorBody);
                        throw new BusinessException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
                    })
                    .body(AiSummaryResponse.class);

            if (body == null) {
                log.warn("AI summary generation returned null response");
                throw new BusinessException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
            }

            return body;

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI summary generation failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
        }
    }
}
