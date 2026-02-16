package com.example.doktoribackend.book.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "isbn13", length = 13, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    private String authors;

    private String publisher;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    private LocalDate publishedAt;

    @Builder
    private Book(String isbn, String title, String authors, String publisher,
                 String thumbnailUrl, LocalDate publishedAt) {
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
    }

    public static Book create(String isbn, String title, String authors,
                              String publisher, String thumbnailUrl, LocalDate publishedAt) {
        return Book.builder()
                .isbn(isbn)
                .title(title)
                .authors(authors)
                .publisher(publisher)
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(publishedAt)
                .build();
    }
}
