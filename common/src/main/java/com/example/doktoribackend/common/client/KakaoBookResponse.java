package com.example.doktoribackend.common.client;

import java.util.List;

public record KakaoBookResponse(
        KakaoBookMeta meta,
        List<KakaoBookDocument> documents
) {
    public record KakaoBookMeta(
            int total_count,
            boolean is_end
    ) {
    }

    public record KakaoBookDocument(
            String title,
            List<String> authors,
            String publisher,
            String thumbnail,
            String datetime,
            String isbn
    ) {
    }
}
