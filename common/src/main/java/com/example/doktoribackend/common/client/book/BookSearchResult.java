package com.example.doktoribackend.common.client.book;

import java.util.List;

public record BookSearchResult(
        List<BookItem> items,
        int totalCount,
        boolean isEnd,
        BookSearchSource source
) {
    public static BookSearchResult empty(BookSearchSource source) {
        return new BookSearchResult(List.of(), 0, true, source);
    }
}
