package com.example.doktoribackend.s3.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchPresignUploadRequest(
        @Valid
        @Size(min = 1, max = 5, message = "파일은 1~5개까지 가능합니다")
        List<PresignUploadRequest> files
) {
}
