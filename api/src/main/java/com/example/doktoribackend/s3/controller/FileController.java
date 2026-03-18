package com.example.doktoribackend.s3.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.s3.dto.BatchPresignUploadRequest;
import com.example.doktoribackend.s3.dto.BatchPresignUploadResponse;
import com.example.doktoribackend.s3.dto.PresignUploadRequest;
import com.example.doktoribackend.s3.dto.PresignUploadResponse;
import com.example.doktoribackend.s3.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileController implements FileApi {

    private final FileService fileService;

    @PostMapping("/presigned-url")
    @Override
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

    @PostMapping("/presigned-urls")
    @Override
    public ResponseEntity<ApiResult<BatchPresignUploadResponse>> batchPresignUpload(
            @Valid @RequestBody BatchPresignUploadRequest request
    ) {
        List<PresignUploadResponse> responses = request.files().stream()
                .map(file -> fileService.presignUpload(
                        file.directory(),
                        file.fileName(),
                        file.contentType(),
                        file.fileSize()
                ))
                .toList();
        return ResponseEntity.ok(ApiResult.ok(new BatchPresignUploadResponse(responses)));
    }
}
