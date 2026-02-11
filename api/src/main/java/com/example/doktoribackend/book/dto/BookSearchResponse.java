package com.example.doktoribackend.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record BookSearchResponse(
        List<BookSearchItem> data,
        PageInfo pageInfo
) {
    public record BookSearchItem(
            String title,
            String authors,
            String publisher,
            String thumbnailUrl,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate publishedAt,
            String isbn
    ) {
    }

    public record PageInfo(
            int page,
            int size,
            int totalCount,
            boolean isEnd
    ) {
    }
}
