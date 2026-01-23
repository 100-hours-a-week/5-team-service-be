package com.example.doktoribackend.book.client;

import com.example.doktoribackend.book.dto.KakaoBookResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoBookClient {

    private final WebClient webClient;

    @Value("${kakao.book.base-url}")
    private String baseUrl;

    @Value("${kakao.book.rest-api-key}")
    private String restApiKey;

    public KakaoBookResponse search(String query, int page, int size) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(baseUrl)
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(KakaoBookResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED));
        } catch (WebClientResponseException ex) {
            log.warn("Kakao book search failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
        } catch (Exception ex) {
            log.warn("Kakao book search failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
        }
    }
}
