package com.example.doktoribackend.s3.dto;

import java.util.List;

public record BatchPresignUploadResponse(
        List<PresignUploadResponse> files
) {
}
