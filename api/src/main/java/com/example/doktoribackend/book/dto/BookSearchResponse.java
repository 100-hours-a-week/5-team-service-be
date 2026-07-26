package com.example.doktoribackend.book.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

public record BookSearchResponse(
        List<BookSearchItem> data,
        PageInfo pageInfo,

        /*
         * 모든 도서 검색 공급자가 실패해 빈 결과로 degrade 된 경우에만 채워진다.
         * 정상적인 "검색 결과 없음" 과 구분하기 위한 필드이므로 평소에는 null 이며 응답에서 생략된다.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notice
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
