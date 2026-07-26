package com.example.doktoribackend.common.client.book;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 카카오 도서 API 장애 시 사용하는 대체 공급자.
 *
 * <p>알라딘은 output=js 응답의 Content-Type 을 text/javascript 로 내려주는 경우가 있어
 * 메시지 컨버터에 맡기지 않고 본문을 문자열로 받아 직접 역직렬화한다.
 */
public class AladinBookSearchClient implements BookSearchClient {

    private static final String API_VERSION = "20131101";
    private static final Pattern ROLE_SUFFIX = Pattern.compile("\\s*\\(([^)]*)\\)\\s*$");
    private static final Set<String> WRITER_ROLES = Set.of("지은이", "지음", "저자", "글", "원작");

    private final RestClient restClient;
    private final String ttbKey;
    private final ObjectMapper objectMapper;

    public AladinBookSearchClient(RestClient restClient, String ttbKey) {
        this.restClient = restClient;
        this.ttbKey = ttbKey;
        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public BookSearchSource source() {
        return BookSearchSource.ALADIN;
    }

    @Override
    @CircuitBreaker(name = "aladinBook")
    @Retry(name = "aladinBook")
    public BookSearchResult search(String query, int page, int size) {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemSearch.aspx")
                        .queryParam("ttbkey", ttbKey)
                        .queryParam("Query", query)
                        .queryParam("QueryType", "Keyword")
                        .queryParam("SearchTarget", "Book")
                        .queryParam("MaxResults", size)
                        .queryParam("start", page)
                        .queryParam("Cover", "Big")
                        .queryParam("output", "js")
                        .queryParam("Version", API_VERSION)
                        .build())
                .retrieve()
                .body(String.class);

        AladinBookResponse response = parse(body);

        int totalCount = response.totalResults() == null ? 0 : response.totalResults();
        int startIndex = response.startIndex() == null ? page : response.startIndex();
        int itemsPerPage = response.itemsPerPage() == null ? size : response.itemsPerPage();

        return new BookSearchResult(
                mapItems(response.item()),
                totalCount,
                (long) startIndex * itemsPerPage >= totalCount,
                BookSearchSource.ALADIN
        );
    }

    @Override
    @CircuitBreaker(name = "aladinBook")
    @Retry(name = "aladinBook")
    public Optional<BookItem> findByIsbn(String isbn) {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemLookUp.aspx")
                        .queryParam("ttbkey", ttbKey)
                        .queryParam("itemIdType", "ISBN13")
                        .queryParam("ItemId", isbn)
                        .queryParam("Cover", "Big")
                        .queryParam("output", "js")
                        .queryParam("Version", API_VERSION)
                        .build())
                .retrieve()
                .body(String.class);

        AladinBookResponse response = parse(body);

        if (response.item() == null || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toItem(response.item().getFirst()));
    }

    private AladinBookResponse parse(String body) {
        if (body == null || body.isBlank()) {
            throw new BookSearchProviderException("알라딘 응답 본문이 비어 있습니다.");
        }

        AladinBookResponse response;
        try {
            response = objectMapper.readValue(body, AladinBookResponse.class);
        } catch (Exception ex) {
            throw new BookSearchProviderException("알라딘 응답 파싱에 실패했습니다.", ex);
        }

        if (response.errorCode() != null) {
            throw new BookSearchProviderException(
                    "알라딘 API 오류 errorCode=" + response.errorCode() + ", message=" + response.errorMessage());
        }
        return response;
    }

    private List<BookItem> mapItems(List<AladinBookResponse.AladinItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(this::toItem).toList();
    }

    private BookItem toItem(AladinBookResponse.AladinItem item) {
        return new BookItem(
                (item.isbn13() == null || item.isbn13().isBlank()) ? item.isbn() : item.isbn13(),
                item.title(),
                parseAuthors(item.author()),
                item.publisher(),
                item.cover(),
                parsePublishedAt(item.pubDate())
        );
    }

    /**
     * 알라딘은 저자를 "홍길동 (지은이), 김철수 (옮긴이)" 형태의 단일 문자열로 준다.
     * 역할 표기가 있으면 지은이만 남기고, 없으면 이름을 그대로 모두 사용한다.
     */
    private String parseAuthors(String author) {
        if (author == null || author.isBlank()) {
            return null;
        }

        List<String> allNames = new ArrayList<>();
        List<String> writers = new ArrayList<>();
        boolean hasRole = false;

        for (String rawToken : author.split(",")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }

            Matcher matcher = ROLE_SUFFIX.matcher(token);
            String name = token;
            String role = null;
            if (matcher.find()) {
                role = matcher.group(1).trim();
                name = token.substring(0, matcher.start()).trim();
                hasRole = true;
            }

            if (name.isEmpty()) {
                continue;
            }
            allNames.add(name);
            if (role != null && WRITER_ROLES.stream().anyMatch(role::contains)) {
                writers.add(name);
            }
        }

        if (hasRole && !writers.isEmpty()) {
            return String.join(", ", writers);
        }
        return allNames.isEmpty() ? null : String.join(", ", allNames);
    }

    private LocalDate parsePublishedAt(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(pubDate.trim().substring(0, 10));
        } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            return null;
        }
    }
}
