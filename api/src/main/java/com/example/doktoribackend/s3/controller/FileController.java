package com.example.doktoribackend.s3.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.s3.dto.PresignUploadRequest;
import com.example.doktoribackend.s3.dto.PresignUploadResponse;
import com.example.doktoribackend.s3.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Upload", description = "S3 이미지 업로드 url")
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드용 사전 서명 URL 발급",
            description = "S3에 직접 업로드하기 위한 presigned URL을 발급합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResult<PresignUploadResponse>> presignUpload(
            @Valid @RequestBody PresignUploadRequest request
    ) {
        PresignUploadResponse response = fileService.presignUpload(
                request.directory(),
                request.fileName(),
                request.contentType(),
                request.fileSize()
        );
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
