package com.example.doktoribackend.book.controller;

import com.example.doktoribackend.book.dto.BookSearchRequest;
import com.example.doktoribackend.book.dto.BookSearchResponse;
import com.example.doktoribackend.book.service.BookSearchService;
import com.example.doktoribackend.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Book", description = "도서 API")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/books")
public class BookController {

    private final BookSearchService bookSearchService;

    @Operation(summary = "도서 검색", description = "검색을 통해 도서 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResult<BookSearchResponse>> searchBooks(
            @Valid @ModelAttribute BookSearchRequest request
    ) {
        BookSearchResponse response = bookSearchService.search(
                request.query(),
                request.getPageOrDefault(),
                request.getSizeOrDefault()
        );
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
