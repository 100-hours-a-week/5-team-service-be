package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.client.KakaoBookClient;
import com.example.doktoribackend.common.client.KakaoBookResponse;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final KakaoBookClient kakaoBookClient;

    public Book resolveBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseGet(() -> kakaoBookClient.searchByIsbn(isbn)
                        .map(doc -> bookRepository.save(toBookFromKakao(doc, isbn)))
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND)));
    }

    private Book toBookFromKakao(KakaoBookResponse.KakaoBookDocument doc, String isbn) {
        String authors = doc.authors() != null ? String.join(", ", doc.authors()) : null;
        LocalDate publishedAt = parsePublishedAt(doc.datetime());
        return Book.create(isbn, doc.title(), authors, doc.publisher(), doc.thumbnail(), publishedAt);
    }

    private LocalDate parsePublishedAt(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(datetime.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }
}
