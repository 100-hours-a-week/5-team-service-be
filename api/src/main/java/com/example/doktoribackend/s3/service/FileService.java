package com.example.doktoribackend.s3.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.common.s3.ImageUrlResolver;
import com.example.doktoribackend.s3.dto.PresignUploadResponse;
import com.example.doktoribackend.s3.dto.UploadDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final ImageUrlResolver imageUrlResolver;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.s3.upload-exp-minutes:5}")
    private long uploadExpMinutes;

    @Value("${app.s3.max-file-size:5242880}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".webp"
    );

    public PresignUploadResponse presignUpload(UploadDirectory directory,
                                               String originalFileName,
                                               String contentType,
                                               Long fileSize) {

        String safeName = Paths.get(originalFileName == null ? "" : originalFileName)
                .getFileName()
                .toString();
        if (safeName.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_NAME_IS_NOT_BLANK);
        }

        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(ErrorCode.INVALID_FILE_SIZE);
        }
        if (fileSize > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.CONTENT_TYPE_NOT_ALLOWED);
        }

        String ext = extractExtension(safeName);
        if (ext.isBlank() || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_EXTENSION);
        }

        if (!isContentTypeMatchExtension(contentType, ext)) {
            throw new BusinessException(ErrorCode.CONTENT_TYPE_MISMATCH);
        }

        String key = buildKey(directory, safeName);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(Duration.ofMinutes(uploadExpMinutes))
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        return new PresignUploadResponse(
                presigned.url().toString(),
                key,
                presigned.signedHeaders()
        );
    }

    public void deleteImageIfChanged(String previousImagePath, String updatedImagePath) {
        if (previousImagePath == null || previousImagePath.isBlank()) {
            return;
        }
        if (Objects.equals(previousImagePath, updatedImagePath)) {
            return;
        }
        if (isExternalUrl(previousImagePath)) {
            return;
        }
        deleteImage(previousImagePath);
    }


    public void deleteImage(String imagePath) {
        String key = extractKey(imagePath);
        if (key == null) {
            throw new BusinessException(ErrorCode.INVALID_S3_KEY);
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return;
            }
            throw new BusinessException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    private boolean isExternalUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }


    private String extractKey(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_IMG_URL);
        }
        String key = imageUrlResolver.extractKey(imagePath);

        if (isExternalUrl(key)) {
            return null;
        }
        if (key.contains("..") || key.contains("//")) {
            throw new BusinessException(ErrorCode.INVALID_S3_KEY);
        }

        if (!isAllowedPrefix(key)) {
            return null;
        }

        return key;
    }

    private String buildKey(UploadDirectory directory, String fileName) {
        String ext = extractExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        return directory.getPath() + "/" + uuid + ext;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot == -1 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase();
    }

    private boolean isAllowedPrefix(String key) {
        return Arrays.stream(UploadDirectory.values())
                .anyMatch(dir -> key.startsWith(dir.getPath() + "/"));
    }

    private boolean isContentTypeMatchExtension(String contentType, String ext) {
        return switch (contentType) {
            case "image/png" -> ".png".equals(ext);
            case "image/jpeg" -> ".jpg".equals(ext) || ".jpeg".equals(ext);
            case "image/webp" -> ".webp".equals(ext);
            default -> false;
        };
    }
}