package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.dto.BookSearchResponse;
import com.example.doktoribackend.common.client.book.BookItem;
import com.example.doktoribackend.common.client.book.BookSearchGateway;
import com.example.doktoribackend.common.client.book.BookSearchResult;
import com.example.doktoribackend.common.client.book.BookSearchSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BookSearchServiceImplTest {

    private BookSearchGateway gateway;
    private BookSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        gateway = mock(BookSearchGateway.class);
        service = new BookSearchServiceImpl(gateway);
    }

    @Test
    @DisplayName("search: 게이트웨이 결과를 응답 DTO로 변환한다")
    void search_success_mapsToResponse() {
        given(gateway.search("clean", 1, 10)).willReturn(new BookSearchResult(
                List.of(new BookItem("9788966260959", "클린 코드", "로버트 C. 마틴",
                        "인사이트", "https://img/clean.jpg", LocalDate.of(2013, 12, 24))),
                25, false, BookSearchSource.KAKAO));

        BookSearchResponse response = service.search("clean", 1, 10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().title()).isEqualTo("클린 코드");
        assertThat(response.data().getFirst().authors()).isEqualTo("로버트 C. 마틴");
        assertThat(response.data().getFirst().isbn()).isEqualTo("9788966260959");
        assertThat(response.data().getFirst().thumbnailUrl()).isEqualTo("https://img/clean.jpg");
        assertThat(response.data().getFirst().publishedAt()).isEqualTo(LocalDate.of(2013, 12, 24));

        assertThat(response.pageInfo().page()).isEqualTo(1);
        assertThat(response.pageInfo().size()).isEqualTo(10);
        assertThat(response.pageInfo().totalCount()).isEqualTo(25);
        assertThat(response.pageInfo().isEnd()).isFalse();
        assertThat(response.notice()).isNull();
    }

    @Test
    @DisplayName("search: 검색 결과가 없어도 공급자가 정상이면 안내 메시지가 없다")
    void search_emptyButHealthy_hasNoNotice() {
        given(gateway.search("없는책", 1, 10))
                .willReturn(BookSearchResult.empty(BookSearchSource.KAKAO));

        BookSearchResponse response = service.search("없는책", 1, 10);

        assertThat(response.data()).isEmpty();
        assertThat(response.notice()).isNull();
    }

    @Test
    @DisplayName("search: 모든 공급자가 실패하면 빈 결과에 안내 메시지를 담아 반환한다")
    void search_allProvidersFailed_includesNotice() {
        given(gateway.search("clean", 1, 10))
                .willReturn(BookSearchResult.empty(BookSearchSource.NONE));

        BookSearchResponse response = service.search("clean", 1, 10);

        assertThat(response.data()).isEmpty();
        assertThat(response.notice()).isNotBlank();
    }
}
