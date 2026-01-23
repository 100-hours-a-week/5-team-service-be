package com.example.doktoribackend.s3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static com.example.doktoribackend.common.constants.ValidationConstant.MAX_FILE_SIZE;

public record PresignUploadRequest(
        @Schema(description = "업로드 디렉터리", example = "PROFILE", allowableValues = {"PROFILE", "MEETING"})
        @NotNull(message = "디렉터리는 필수입니다")
        UploadDirectory directory,

        @Schema(description = "파일 이름", example = "profile.jpg")
        @NotBlank(message = "파일 이름은 필수입니다")
        String fileName,

        @Schema(description = "콘텐츠 타입", example = "image/jpeg", allowableValues = {"image/png", "image/jpeg", "image/webp"})
        @NotBlank(message = "콘텐츠 타입은 필수입니다")
        String contentType,

        @Schema(description = "파일 크기 (바이트)", example = "1048576")
        @NotNull(message = "파일 크기는 필수입니다")
        @Positive(message = "파일 크기는 0보다 커야 합니다")
        @Max(value = MAX_FILE_SIZE, message = "파일 크기는 5MB를 초과할 수 없습니다")
        Long fileSize
) {}