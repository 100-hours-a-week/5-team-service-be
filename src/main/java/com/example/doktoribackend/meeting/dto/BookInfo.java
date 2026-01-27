package com.example.doktoribackend.meeting.dto;

import com.example.doktoribackend.book.domain.Book;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BookInfo {
    private String title;
    private String authors;
    private String publisher;
    private String thumbnailUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishedAt;

    public static BookInfo from(Book book) {
        return BookInfo.builder()
                .title(book.getTitle())
                .authors(book.getAuthors())
                .publisher(book.getPublisher())
                .thumbnailUrl(book.getThumbnailUrl())
                .publishedAt(book.getPublishedAt())
                .build();
    }
}