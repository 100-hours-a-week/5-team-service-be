package com.example.doktoribackend.s3.dto;

import java.util.List;
import java.util.Map;

public record PresignUploadResponse (
        String uploadUrl,
        String key,
        Map<String, List<String>> headers
){}