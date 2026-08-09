package com.example.doktoribackend.common.client.book;

import java.util.Optional;

/**
 * 도서 검색 공급자 추상화.
 *
 * <p>구현체는 실패를 삼키지 않는다. 5xx 는 {@code HttpServerErrorException},
 * 타임아웃/네트워크 오류는 {@code ResourceAccessException} 으로 그대로 전파해야
 * Resilience4j 의 recordExceptions 가 실패로 집계할 수 있다.
 * 사용자에게 보여줄 degrade 는 {@link BookSearchGateway} 가 담당한다.
 */
public interface BookSearchClient {

    BookSearchResult search(String query, int page, int size);

    Optional<BookItem> findByIsbn(String isbn);

    BookSearchSource source();
}
