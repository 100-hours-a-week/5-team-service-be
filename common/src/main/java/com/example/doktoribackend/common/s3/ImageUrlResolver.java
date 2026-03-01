package com.example.doktoribackend.common.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlResolver {

    private final String baseUrl;

    public ImageUrlResolver(@Value("${app.s3.public-base-url}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public String toUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }
        String trimmed = keyOrUrl.trim();

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        if (baseUrl.isEmpty()) {
            return trimmed;
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl + trimmed;
        }
        return baseUrl + "/" + trimmed;
    }

    public String extractKey(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }
        String candidate = keyOrUrl.trim();

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            if (!baseUrl.isEmpty() && candidate.startsWith(baseUrl)) {
                candidate = candidate.substring(baseUrl.length());
                if (candidate.startsWith("/")) {
                    candidate = candidate.substring(1);
                }
            } else {
                return candidate;
            }
        }

        if (candidate.startsWith("/")) {
            candidate = candidate.substring(1);
        }

        return candidate;
    }
}
