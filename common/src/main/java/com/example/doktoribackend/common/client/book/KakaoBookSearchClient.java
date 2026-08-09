package com.example.doktoribackend.common.client.book;

import com.example.doktoribackend.common.client.KakaoBookResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class KakaoBookSearchClient implements BookSearchClient {

    private final RestClient restClient;

    public KakaoBookSearchClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public BookSearchSource source() {
        return BookSearchSource.KAKAO;
    }

    @Override
    @CircuitBreaker(name = "kakaoBook")
    @Retry(name = "kakaoBook")
    public BookSearchResult search(String query, int page, int size) {
        KakaoBookResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(KakaoBookResponse.class);

        if (response == null || response.meta() == null) {
            return BookSearchResult.empty(BookSearchSource.KAKAO);
        }

        return new BookSearchResult(
                mapItems(response.documents()),
                response.meta().total_count(),
                response.meta().is_end(),
                BookSearchSource.KAKAO
        );
    }

    @Override
    @CircuitBreaker(name = "kakaoBook")
    @Retry(name = "kakaoBook")
    public Optional<BookItem> findByIsbn(String isbn) {
        KakaoBookResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("query", isbn)
                        .queryParam("target", "isbn")
                        .queryParam("size", 1)
                        .build())
                .retrieve()
                .body(KakaoBookResponse.class);

        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toItem(response.documents().getFirst()));
    }

    private List<BookItem> mapItems(List<KakaoBookResponse.KakaoBookDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream().map(this::toItem).toList();
    }

    private BookItem toItem(KakaoBookResponse.KakaoBookDocument doc) {
        String authors = (doc.authors() == null || doc.authors().isEmpty())
                ? null
                : String.join(", ", doc.authors());
        return new BookItem(
                extractIsbn(doc.isbn()),
                doc.title(),
                authors,
                doc.publisher(),
                doc.thumbnail(),
                parsePublishedAt(doc.datetime())
        );
    }

    private LocalDate parsePublishedAt(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(datetime).toLocalDate();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * 카카오는 ISBN10 과 ISBN13 을 공백으로 구분해 한 필드에 담아준다. ISBN13 을 우선한다.
     */
    private String extractIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        String isbn10 = null;
        for (String token : isbn.trim().split("\\s+")) {
            if (token.length() == 13 && token.chars().allMatch(Character::isDigit)) {
                return token;
            }
            if (token.length() == 10 && token.chars().allMatch(Character::isDigit)) {
                isbn10 = token;
            }
        }
        return isbn10;
    }
}
