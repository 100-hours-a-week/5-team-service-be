package com.example.doktoribackend.common.client.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.hamcrest.Matchers.startsWith;

class KakaoBookSearchClientTest {

    private static final String BASE_URL = "https://dapi.kakao.com/v3/search/book";
    private static final String API_KEY = "test-rest-api-key";

    private MockRestServiceServer server;
    private KakaoBookSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "KakaoAK " + API_KEY);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoBookSearchClient(builder.build());
    }

    @Test
    @DisplayName("search: 카카오 응답을 공통 도메인 모델로 변환한다")
    void search_success_mapsToCommonModel() {
        server.expect(once(), requestTo(startsWith(BASE_URL)))
                .andExpect(queryParam("query", "clean"))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("size", "10"))
                .andExpect(header("Authorization", "KakaoAK " + API_KEY))
                .andRespond(withSuccess("""
                        {
                          "meta": {"total_count": 2, "is_end": false},
                          "documents": [
                            {
                              "title": "클린 코드",
                              "authors": ["로버트 C. 마틴", "박재호"],
                              "publisher": "인사이트",
                              "thumbnail": "https://img.kakao/clean.jpg",
                              "datetime": "2013-12-24T00:00:00.000+09:00",
                              "isbn": "8966260959 9788966260959"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        BookSearchResult result = client.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.KAKAO);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.isEnd()).isFalse();
        assertThat(result.items()).hasSize(1);

        BookItem item = result.items().getFirst();
        assertThat(item.title()).isEqualTo("클린 코드");
        assertThat(item.authors()).isEqualTo("로버트 C. 마틴, 박재호");
        assertThat(item.publisher()).isEqualTo("인사이트");
        assertThat(item.thumbnailUrl()).isEqualTo("https://img.kakao/clean.jpg");
        assertThat(item.publishedAt()).isEqualTo(LocalDate.of(2013, 12, 24));
        assertThat(item.isbn()).isEqualTo("9788966260959");

        server.verify();
    }

    @Test
    @DisplayName("search: 5xx 응답은 HttpServerErrorException으로 전파한다 (서킷 집계 대상)")
    void search_serverError_propagatesHttpServerErrorException() {
        server.expect(once(), requestTo(startsWith(BASE_URL)))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("clean", 1, 10))
                .isInstanceOf(HttpServerErrorException.class);

        server.verify();
    }

    @Test
    @DisplayName("findByIsbn: 검색 결과가 없으면 Optional.empty를 반환한다")
    void findByIsbn_noDocuments_returnsEmpty() {
        server.expect(once(), requestTo(startsWith(BASE_URL)))
                .andExpect(queryParam("target", "isbn"))
                .andRespond(withSuccess("""
                        {"meta": {"total_count": 0, "is_end": true}, "documents": []}
                        """, MediaType.APPLICATION_JSON));

        Optional<BookItem> found = client.findByIsbn("9788966260959");

        assertThat(found).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("findByIsbn: 첫 번째 문서를 공통 모델로 변환해 반환한다")
    void findByIsbn_found_returnsFirstItem() {
        server.expect(once(), requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess("""
                        {
                          "meta": {"total_count": 1, "is_end": true},
                          "documents": [
                            {
                              "title": "클린 코드",
                              "authors": ["로버트 C. 마틴"],
                              "publisher": "인사이트",
                              "thumbnail": "https://img.kakao/clean.jpg",
                              "datetime": "2013-12-24T00:00:00.000+09:00",
                              "isbn": "8966260959 9788966260959"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<BookItem> found = client.findByIsbn("9788966260959");

        assertThat(found).isPresent();
        assertThat(found.get().isbn()).isEqualTo("9788966260959");
        assertThat(found.get().authors()).isEqualTo("로버트 C. 마틴");
        server.verify();
    }
}
