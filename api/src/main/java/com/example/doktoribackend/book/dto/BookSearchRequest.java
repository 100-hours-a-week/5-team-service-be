package com.example.doktoribackend.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookSearchRequest(
        @NotBlank String query,
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer size
) {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    public int getPageOrDefault() {
        return page != null ? page : DEFAULT_PAGE;
    }

    public int getSizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }
}
