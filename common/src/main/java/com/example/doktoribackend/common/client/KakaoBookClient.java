package com.example.doktoribackend.common.client;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Slf4j
public class KakaoBookClient {

    private final RestClient restClient;

    public KakaoBookClient(
            @Value("${kakao.book.base-url}") String baseUrl,
            @Value("${kakao.book.rest-api-key}") String restApiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "KakaoAK " + restApiKey)
                .build();
    }

    public KakaoBookResponse search(String query, int page, int size) {
        try {
            KakaoBookResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Kakao book search failed: status={}", res.getStatusCode());
                        throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
                    })
                    .body(KakaoBookResponse.class);

            if (response == null) {
                throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UPSTREAM_KAKAO_FAILED);
        }
    }

    public Optional<KakaoBookResponse.KakaoBookDocument> searchByIsbn(String isbn) {
        try {
            KakaoBookResponse body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", isbn)
                            .queryParam("target", "isbn")
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) ->
                            log.warn("Kakao book search by ISBN failed: isbn={}, status={}", isbn, res.getStatusCode()))
                    .body(KakaoBookResponse.class);

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
