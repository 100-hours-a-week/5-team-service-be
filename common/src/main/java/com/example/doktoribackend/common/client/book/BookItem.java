package com.example.doktoribackend.common.client.book;

import java.time.LocalDate;

/**
 * 공급자(카카오/알라딘)에 무관한 도서 공통 모델.
 */
public record BookItem(
        String isbn,
        String title,
        String authors,
        String publisher,
        String thumbnailUrl,
        LocalDate publishedAt
) {
}
