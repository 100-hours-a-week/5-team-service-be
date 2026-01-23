package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.client.KakaoBookClient;
import com.example.doktoribackend.book.dto.BookSearchResponse;
import com.example.doktoribackend.book.dto.KakaoBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookSearchServiceImpl implements BookSearchService {

    private final KakaoBookClient kakaoBookClient;

    @Override
    public BookSearchResponse search(String query, int page, int size) {
        KakaoBookResponse kakaoResponse = kakaoBookClient.search(query, page, size);
        List<BookSearchResponse.BookSearchItem> items = mapItems(kakaoResponse.documents());
        BookSearchResponse.PageInfo pageInfo = new BookSearchResponse.PageInfo(
                page,
                size,
                kakaoResponse.meta().total_count(),
                kakaoResponse.meta().is_end()
        );
        return new BookSearchResponse(items, pageInfo);
    }

    private List<BookSearchResponse.BookSearchItem> mapItems(List<KakaoBookResponse.KakaoBookDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return documents.stream()
                .map(this::toItem)
                .toList();
    }

    private BookSearchResponse.BookSearchItem toItem(KakaoBookResponse.KakaoBookDocument doc) {
        String authors = (doc.authors() == null || doc.authors().isEmpty())
                ? null
                : String.join(", ", doc.authors());
        LocalDate publishedAt = parsePublishedAt(doc.datetime());
        String isbn13 = extractIsbn13(doc.isbn());
        return new BookSearchResponse.BookSearchItem(
                doc.title(),
                authors,
                doc.publisher(),
                doc.thumbnail(),
                publishedAt,
                isbn13
        );
    }

    private LocalDate parsePublishedAt(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(datetime).toLocalDate();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String extractIsbn13(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }
        String[] tokens = isbn.trim().split("\\s+");
        for (String token : tokens) {
            if (token.length() == 13 && token.chars().allMatch(Character::isDigit)) {
                return token;
            }
        }
        return null;
    }
}
