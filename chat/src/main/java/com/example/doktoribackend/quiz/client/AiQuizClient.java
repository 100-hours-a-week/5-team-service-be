package com.example.doktoribackend.quiz.client;

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
public class AiQuizClient {

    private final RestClient aiRestClient;

    public AiQuizGenerateResponse generate(AiQuizGenerateRequest request) {
        try {
            AiQuizGenerateResponse body = aiRestClient.post()
                    .uri("/quiz/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errorBody = new String(res.getBody().readAllBytes());
                        log.warn("AI quiz generation failed: status={}, body={}", res.getStatusCode(), errorBody);
                        throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED);
                    })
                    .body(AiQuizGenerateResponse.class);

            if (body == null) {
                log.warn("AI quiz generation returned null response");
                throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED);
            }

            return body;

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI quiz generation failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_QUIZ_GENERATION_FAILED);
        }
    }
}
