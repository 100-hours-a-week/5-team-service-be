package com.example.doktoribackend.common.client.book;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 도서 검색 공급자 fallback 체인.
 *
 * <p>서킷 브레이커와 재시도는 각 {@link BookSearchClient} 구현체에 걸려 있고,
 * 이 클래스는 "어떤 공급자로 넘어갈지"만 결정한다. 서킷이 Open 이면 구현체가
 * CallNotPermittedException 을 즉시 던지므로 여기서 다음 공급자로 넘어간다.
 *
 * <p>정상 응답의 빈 결과는 실패가 아니므로 fallback 하지 않는다. 검색어에 해당하는
 * 책이 없는 것과 공급자가 죽은 것은 다른 상황이다.
 */
@Slf4j
public class BookSearchGateway {

    private final BookSearchClient primary;
    private final BookSearchClient secondary;

    public BookSearchGateway(BookSearchClient primary, BookSearchClient secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    public BookSearchResult search(String query, int page, int size) {
        try {
            return primary.search(query, page, size);
        } catch (Exception ex) {
            log.warn("도서 검색 1차 공급자 실패, fallback 시도 - source={}, error={}: {}",
                    primary.source(), ex.getClass().getSimpleName(), ex.getMessage());
        }

        try {
            return secondary.search(query, page, size);
        } catch (Exception ex) {
            log.error("도서 검색 전 공급자 실패, 빈 결과로 degrade - source={}, error={}: {}",
                    secondary.source(), ex.getClass().getSimpleName(), ex.getMessage());
        }

        return BookSearchResult.empty(BookSearchSource.NONE);
    }

    public Optional<BookItem> findByIsbn(String isbn) {
        try {
            return primary.findByIsbn(isbn);
        } catch (Exception ex) {
            log.warn("ISBN 조회 1차 공급자 실패, fallback 시도 - source={}, isbn={}, error={}: {}",
                    primary.source(), isbn, ex.getClass().getSimpleName(), ex.getMessage());
        }

        try {
            return secondary.findByIsbn(isbn);
        } catch (Exception ex) {
            log.error("ISBN 조회 전 공급자 실패 - source={}, isbn={}, error={}: {}",
                    secondary.source(), isbn, ex.getClass().getSimpleName(), ex.getMessage());
        }

        return Optional.empty();
    }
}
