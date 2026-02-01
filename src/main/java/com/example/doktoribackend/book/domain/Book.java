package com.example.doktoribackend.book.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_book_title_deleted_at", columnList = "title,deleted_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Book extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 13, unique = true)
    private String isbn13;

    @Column(nullable = false)
    private String title;

    @Column(length = 255)
    private String authors;

    @Column(length = 255)
    private String publisher;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    private LocalDate publishedAt;

    @Column(length = 2000)
    private String summary;

    private LocalDateTime deletedAt;

    public static Book create(
            String isbn13,
            String title,
            String authors,
            String publisher,
            String thumbnailUrl,
            LocalDate publishedAt
    ) {
        return Book.builder()
                .isbn13(isbn13)
                .title(title)
                .authors(authors)
                .publisher(publisher)
                .thumbnailUrl(thumbnailUrl)
                .publishedAt(publishedAt)
                .build();
    }

    public void revive() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
