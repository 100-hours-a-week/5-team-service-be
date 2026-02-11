package com.example.doktoribackend.book.service;

import com.example.doktoribackend.book.dto.BookSearchResponse;

public interface BookSearchService {
    BookSearchResponse search(String query, int page, int size);
}
