package com.example.doktoribackend.common.client.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AladinBookSearchClientTest {

    private static final String BASE_URL = "https://www.aladin.co.kr/ttb/api";
    private static final String TTB_KEY = "test-ttb-key";

    private MockRestServiceServer server;
    private AladinBookSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AladinBookSearchClient(builder.build(), TTB_KEY);
    }

    @Test
    @DisplayName("search: 알라딘 응답을 카카오와 동일한 공통 모델로 변환한다")
    void search_success_mapsToCommonModel() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andExpect(queryParam("ttbkey", TTB_KEY))
                .andExpect(queryParam("Query", "clean"))
                .andExpect(queryParam("MaxResults", "10"))
                .andExpect(queryParam("start", "1"))
                .andExpect(queryParam("output", "js"))
                .andRespond(withSuccess("""
                        {
                          "totalResults": 25,
                          "startIndex": 1,
                          "itemsPerPage": 10,
                          "item": [
                            {
                              "title": "클린 코드",
                              "author": "로버트 C. 마틴 (지은이), 박재호 (옮긴이)",
                              "publisher": "인사이트",
                              "pubDate": "2013-12-24",
                              "cover": "https://image.aladin.co.kr/clean.jpg",
                              "isbn": "8966260959",
                              "isbn13": "9788966260959"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        BookSearchResult result = client.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.ALADIN);
        assertThat(result.totalCount()).isEqualTo(25);
        assertThat(result.isEnd()).isFalse();
        assertThat(result.items()).hasSize(1);

        BookItem item = result.items().getFirst();
        assertThat(item.title()).isEqualTo("클린 코드");
        assertThat(item.publisher()).isEqualTo("인사이트");
        assertThat(item.thumbnailUrl()).isEqualTo("https://image.aladin.co.kr/clean.jpg");
        assertThat(item.publishedAt()).isEqualTo(LocalDate.of(2013, 12, 24));
        assertThat(item.isbn()).isEqualTo("9788966260959");

        server.verify();
    }

    @Test
    @DisplayName("search: 저자 문자열에서 역할 표기를 제거하고 지은이만 남긴다")
    void search_authorWithRoles_keepsOnlyWriters() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andRespond(withSuccess(aladinBody("로버트 C. 마틴 (지은이), 박재호 (옮긴이)"), MediaType.APPLICATION_JSON));

        BookSearchResult result = client.search("clean", 1, 10);

        assertThat(result.items().getFirst().authors()).isEqualTo("로버트 C. 마틴");
    }

    @Test
    @DisplayName("search: 역할 표기가 없으면 저자 이름을 그대로 모두 사용한다")
    void search_authorWithoutRoles_keepsAllNames() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andRespond(withSuccess(aladinBody("로버트 C. 마틴, 박재호"), MediaType.APPLICATION_JSON));

        BookSearchResult result = client.search("clean", 1, 10);

        assertThat(result.items().getFirst().authors()).isEqualTo("로버트 C. 마틴, 박재호");
    }

    @Test
    @DisplayName("search: 마지막 페이지면 isEnd가 true다")
    void search_lastPage_isEndTrue() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andRespond(withSuccess("""
                        {"totalResults": 20, "startIndex": 2, "itemsPerPage": 10, "item": []}
                        """, MediaType.APPLICATION_JSON));

        BookSearchResult result = client.search("clean", 2, 10);

        assertThat(result.isEnd()).isTrue();
    }

    @Test
    @DisplayName("search: HTTP 200이라도 errorCode 페이로드면 예외를 던진다 (쿼터 초과·잘못된 키)")
    void search_errorPayloadWithHttp200_throws() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andRespond(withSuccess("""
                        {"errorCode": 8, "errorMessage": "Invalid TTBKey or over the daily limit"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("clean", 1, 10))
                .isInstanceOf(BookSearchProviderException.class)
                .hasMessageContaining("8");
    }

    @Test
    @DisplayName("search: Content-Type이 text/javascript여도 파싱한다 (알라딘 output=js 응답 특성)")
    void search_textJavascriptContentType_parsesBody() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemSearch.aspx")))
                .andRespond(withSuccess(aladinBody("로버트 C. 마틴 (지은이)"),
                        MediaType.valueOf("text/javascript;charset=UTF-8")));

        BookSearchResult result = client.search("clean", 1, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().title()).isEqualTo("클린 코드");
    }

    @Test
    @DisplayName("findByIsbn: ItemLookUp 결과를 공통 모델로 반환한다")
    void findByIsbn_found_returnsItem() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemLookUp.aspx")))
                .andExpect(queryParam("ItemId", "9788966260959"))
                .andExpect(queryParam("itemIdType", "ISBN13"))
                .andRespond(withSuccess(aladinBody("로버트 C. 마틴 (지은이)"), MediaType.APPLICATION_JSON));

        Optional<BookItem> found = client.findByIsbn("9788966260959");

        assertThat(found).isPresent();
        assertThat(found.get().isbn()).isEqualTo("9788966260959");
        server.verify();
    }

    @Test
    @DisplayName("findByIsbn: item이 비어 있으면 Optional.empty를 반환한다")
    void findByIsbn_emptyItem_returnsEmpty() {
        server.expect(once(), requestTo(startsWith(BASE_URL + "/ItemLookUp.aspx")))
                .andRespond(withSuccess("""
                        {"totalResults": 0, "startIndex": 1, "itemsPerPage": 10, "item": []}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.findByIsbn("9788966260959")).isEmpty();
    }

    private String aladinBody(String author) {
        return """
                {
                  "totalResults": 1,
                  "startIndex": 1,
                  "itemsPerPage": 10,
                  "item": [
                    {
                      "title": "클린 코드",
                      "author": "%s",
                      "publisher": "인사이트",
                      "pubDate": "2013-12-24",
                      "cover": "https://image.aladin.co.kr/clean.jpg",
                      "isbn": "8966260959",
                      "isbn13": "9788966260959"
                    }
                  ]
                }
                """.formatted(author);
    }
}
