package com.example.doktoribackend.book.client;

import com.example.doktoribackend.book.dto.KakaoBookResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoBookClient {

    private final RestTemplate restTemplate;

    @Value("${kakao.book.base-url}")
    private String baseUrl;

    @Value("${kakao.book.rest-api-key}")
    private String restApiKey;

    public KakaoBookResponse search(String query, int page, int size) {
        String url = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v3/search/book")
                .queryParam("query", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoBookResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    KakaoBookResponse.class
            );

            if (response.getBody() == null) {
                throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
            }
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Kakao book search failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
        }
    }

    public Optional<KakaoBookResponse.KakaoBookDocument> searchByIsbn(String isbn) {
        String url = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v3/search/book")
                .queryParam("query", isbn)
                .queryParam("target", "isbn")
                .queryParam("size", 1)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoBookResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    KakaoBookResponse.class
            );

            KakaoBookResponse body = response.getBody();
            if (body == null || body.documents() == null || body.documents().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(body.documents().getFirst());
        } catch (Exception ex) {
            log.warn("Kakao book search by ISBN failed: isbn={}, error={}", isbn, ex.getMessage());
            return Optional.empty();
        }
    }
}
