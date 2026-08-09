package com.example.doktoribackend.common.client.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class BookSearchGatewayTest {

    private BookSearchClient kakao;
    private BookSearchClient aladin;
    private BookSearchGateway gateway;

    @BeforeEach
    void setUp() {
        kakao = mock(BookSearchClient.class);
        aladin = mock(BookSearchClient.class);
        given(kakao.source()).willReturn(BookSearchSource.KAKAO);
        given(aladin.source()).willReturn(BookSearchSource.ALADIN);
        gateway = new BookSearchGateway(kakao, aladin);
    }

    @Test
    @DisplayName("search: 카카오가 성공하면 알라딘을 호출하지 않는다")
    void search_primarySucceeds_doesNotCallSecondary() {
        given(kakao.search("clean", 1, 10)).willReturn(result(BookSearchSource.KAKAO));

        BookSearchResult result = gateway.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.KAKAO);
        verify(aladin, never()).search(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("search: 카카오 결과가 0건이어도 정상 응답이면 알라딘으로 넘어가지 않는다")
    void search_primaryReturnsEmptyButSucceeds_doesNotFallback() {
        given(kakao.search("없는책", 1, 10)).willReturn(BookSearchResult.empty(BookSearchSource.KAKAO));

        BookSearchResult result = gateway.search("없는책", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.KAKAO);
        assertThat(result.items()).isEmpty();
        verify(aladin, never()).search(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("search: 카카오가 5xx로 실패하면 알라딘 결과를 반환한다")
    void search_primaryFails_fallsBackToSecondary() {
        given(kakao.search("clean", 1, 10))
                .willThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));
        given(aladin.search("clean", 1, 10)).willReturn(result(BookSearchSource.ALADIN));

        BookSearchResult result = gateway.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.ALADIN);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("search: 카카오가 타임아웃이면 알라딘 결과를 반환한다")
    void search_primaryTimesOut_fallsBackToSecondary() {
        given(kakao.search("clean", 1, 10))
                .willThrow(new ResourceAccessException("timeout", new SocketTimeoutException()));
        given(aladin.search("clean", 1, 10)).willReturn(result(BookSearchSource.ALADIN));

        BookSearchResult result = gateway.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.ALADIN);
    }

    @Test
    @DisplayName("search: 두 공급자가 모두 실패하면 예외 대신 빈 결과로 degrade한다")
    void search_allProvidersFail_degradesToEmptyResult() {
        given(kakao.search("clean", 1, 10))
                .willThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));
        given(aladin.search("clean", 1, 10))
                .willThrow(new BookSearchProviderException("알라딘 API 오류 errorCode=8"));

        BookSearchResult result = gateway.search("clean", 1, 10);

        assertThat(result.source()).isEqualTo(BookSearchSource.NONE);
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.isEnd()).isTrue();
    }

    @Test
    @DisplayName("findByIsbn: 카카오 실패 시 알라딘으로 조회한다")
    void findByIsbn_primaryFails_fallsBackToSecondary() {
        given(kakao.findByIsbn("9788966260959"))
                .willThrow(new ResourceAccessException("timeout", new SocketTimeoutException()));
        given(aladin.findByIsbn("9788966260959")).willReturn(Optional.of(bookItem()));

        Optional<BookItem> found = gateway.findByIsbn("9788966260959");

        assertThat(found).isPresent();
        assertThat(found.get().isbn()).isEqualTo("9788966260959");
    }

    @Test
    @DisplayName("findByIsbn: 카카오가 정상적으로 '없음'을 반환하면 알라딘을 호출하지 않는다")
    void findByIsbn_primaryReturnsEmptySuccessfully_doesNotFallback() {
        given(kakao.findByIsbn("9788966260959")).willReturn(Optional.empty());

        Optional<BookItem> found = gateway.findByIsbn("9788966260959");

        assertThat(found).isEmpty();
        verify(aladin, never()).findByIsbn(anyString());
    }

    @Test
    @DisplayName("findByIsbn: 두 공급자가 모두 실패하면 Optional.empty를 반환한다")
    void findByIsbn_allProvidersFail_returnsEmpty() {
        given(kakao.findByIsbn("9788966260959"))
                .willThrow(HttpServerErrorException.create(INTERNAL_SERVER_ERROR, "err", null, null, null));
        given(aladin.findByIsbn("9788966260959"))
                .willThrow(new BookSearchProviderException("알라딘 API 오류"));

        assertThat(gateway.findByIsbn("9788966260959")).isEmpty();
    }

    private BookSearchResult result(BookSearchSource source) {
        return new BookSearchResult(List.of(bookItem()), 1, true, source);
    }

    private BookItem bookItem() {
        return new BookItem("9788966260959", "클린 코드", "로버트 C. 마틴",
                "인사이트", "https://img/clean.jpg", LocalDate.of(2013, 12, 24));
    }
}
