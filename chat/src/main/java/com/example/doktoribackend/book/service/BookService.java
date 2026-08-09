package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.book.repository.BookRepository;
import com.example.doktoribackend.common.client.book.BookItem;
import com.example.doktoribackend.common.client.book.BookSearchGateway;
import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookSearchGateway bookSearchGateway;

    public Book resolveBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseGet(() -> bookSearchGateway.findByIsbn(isbn)
                        .map(item -> bookRepository.save(toBook(item, isbn)))
                        .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND)));
    }

    private Book toBook(BookItem item, String isbn) {
        return Book.create(
                isbn,
                item.title(),
                item.authors(),
                item.publisher(),
                item.thumbnailUrl(),
                item.publishedAt()
        );
    }
}
