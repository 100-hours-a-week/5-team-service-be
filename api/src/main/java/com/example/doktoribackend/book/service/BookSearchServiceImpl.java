package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.dto.BookSearchResponse;
import com.example.doktoribackend.common.client.book.BookItem;
import com.example.doktoribackend.common.client.book.BookSearchGateway;
import com.example.doktoribackend.common.client.book.BookSearchResult;
import com.example.doktoribackend.common.client.book.BookSearchSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookSearchServiceImpl implements BookSearchService {

    private static final String DEGRADED_NOTICE =
            "도서 검색 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요.";

    private final BookSearchGateway bookSearchGateway;

    @Override
    public BookSearchResponse search(String query, int page, int size) {
        BookSearchResult result = bookSearchGateway.search(query, page, size);

        BookSearchResponse.PageInfo pageInfo = new BookSearchResponse.PageInfo(
                page,
                size,
                result.totalCount(),
                result.isEnd()
        );

        String notice = result.source() == BookSearchSource.NONE ? DEGRADED_NOTICE : null;

        return new BookSearchResponse(mapItems(result.items()), pageInfo, notice);
    }

    private List<BookSearchResponse.BookSearchItem> mapItems(List<BookItem> items) {
        return items.stream()
                .map(item -> new BookSearchResponse.BookSearchItem(
                        item.title(),
                        item.authors(),
                        item.publisher(),
                        item.thumbnailUrl(),
                        item.publishedAt(),
                        item.isbn()
                ))
                .toList();
    }
}
