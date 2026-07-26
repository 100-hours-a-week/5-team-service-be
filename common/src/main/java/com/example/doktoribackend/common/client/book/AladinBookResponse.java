package com.example.doktoribackend.common.client.book;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 알라딘 ItemSearch / ItemLookUp 응답. 실제 응답에는 다수의 부가 필드가 있어 미지정 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AladinBookResponse(
        Integer totalResults,
        Integer startIndex,
        Integer itemsPerPage,
        Integer errorCode,
        String errorMessage,
        List<AladinItem> item
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinItem(
            String title,
            String author,
            String publisher,
            String pubDate,
            String cover,
            String isbn,
            String isbn13
    ) {
    }
}
