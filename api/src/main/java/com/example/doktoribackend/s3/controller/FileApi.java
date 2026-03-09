package com.example.doktoribackend.s3.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.s3.dto.BatchPresignUploadRequest;
import com.example.doktoribackend.s3.dto.BatchPresignUploadResponse;
import com.example.doktoribackend.s3.dto.PresignUploadRequest;
import com.example.doktoribackend.s3.dto.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Upload", description = "S3 이미지 업로드 API")
public interface FileApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "파일 업로드용 사전 서명 URL 발급",
            description = "S3에 직접 업로드하기 위한 presigned URL을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "uploadUrl": "https://s3.amazonaws.com/bucket/images/profiles/uuid.jpg?...",
                                "key": "images/profiles/uuid.jpg",
                                "headers": {
                                  "Content-Type": ["image/jpeg"],
                                  "Cache-Control": ["public, max-age=31536000, immutable"]
                                }
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "파일 이름 공백",
                                    value = """
                                            {
                                              "code": "FILE_NAME_IS_NOT_BLANK",
                                              "message": "파일 이름은 공백일 수 없습니다."
                                            }
                                            """),
                            @ExampleObject(name = "허용되지 않는 콘텐츠 타입",
                                    value = """
                                            {
                                              "code": "CONTENT_TYPE_NOT_ALLOWED",
                                              "message": "허용되지 않는 컨텐츠 타입입니다."
                                            }
                                            """),
                            @ExampleObject(name = "파일 크기 초과",
                                    value = """
                                            {
                                              "code": "FILE_SIZE_EXCEEDED",
                                              "message": "허용된 파일 크기를 초과했습니다."
                                            }
                                            """),
                            @ExampleObject(name = "허용되지 않은 확장자",
                                    value = """
                                            {
                                              "code": "INVALID_FILE_EXTENSION",
                                              "message": "허용되지 않은 파일 확장자입니다."
                                            }
                                            """),
                            @ExampleObject(name = "확장자-타입 불일치",
                                    value = """
                                            {
                                              "code": "CONTENT_TYPE_MISMATCH",
                                              "message": "파일 확장자와 콘텐츠 타입이 일치하지 않습니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<PresignUploadResponse>> presignUpload(PresignUploadRequest request);

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "다건 파일 업로드용 사전 서명 URL 발급",
            description = "여러 파일(최대 5개)에 대한 presigned URL을 한 번에 발급합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "files": [
                                  {
                                    "uploadUrl": "https://s3.amazonaws.com/bucket/images/reviews/uuid1.jpg?...",
                                    "key": "images/reviews/uuid1.jpg",
                                    "headers": {
                                      "Content-Type": ["image/jpeg"]
                                    }
                                  },
                                  {
                                    "uploadUrl": "https://s3.amazonaws.com/bucket/images/reviews/uuid2.png?...",
                                    "key": "images/reviews/uuid2.png",
                                    "headers": {
                                      "Content-Type": ["image/png"]
                                    }
                                  }
                                ]
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "파일 이름 공백",
                                    value = """
                                            {
                                              "code": "FILE_NAME_IS_NOT_BLANK",
                                              "message": "파일 이름은 공백일 수 없습니다."
                                            }
                                            """),
                            @ExampleObject(name = "허용되지 않는 콘텐츠 타입",
                                    value = """
                                            {
                                              "code": "CONTENT_TYPE_NOT_ALLOWED",
                                              "message": "허용되지 않는 컨텐츠 타입입니다."
                                            }
                                            """),
                            @ExampleObject(name = "파일 크기 초과",
                                    value = """
                                            {
                                              "code": "FILE_SIZE_EXCEEDED",
                                              "message": "허용된 파일 크기를 초과했습니다."
                                            }
                                            """),
                            @ExampleObject(name = "허용되지 않은 확장자",
                                    value = """
                                            {
                                              "code": "INVALID_FILE_EXTENSION",
                                              "message": "허용되지 않은 파일 확장자입니다."
                                            }
                                            """),
                            @ExampleObject(name = "확장자-타입 불일치",
                                    value = """
                                            {
                                              "code": "CONTENT_TYPE_MISMATCH",
                                              "message": "파일 확장자와 콘텐츠 타입이 일치하지 않습니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<BatchPresignUploadResponse>> batchPresignUpload(BatchPresignUploadRequest request);
}
