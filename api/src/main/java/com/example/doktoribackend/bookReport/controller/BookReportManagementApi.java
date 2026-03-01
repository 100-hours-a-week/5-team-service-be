package com.example.doktoribackend.bookReport.controller;

import com.example.doktoribackend.bookReport.dto.BookReportManagementResponse;
import com.example.doktoribackend.bookReport.dto.MemberBookReportDetailResponse;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface BookReportManagementApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            summary = "독후감 관리 조회",
            description = "모임장이 특정 회차의 독후감 제출 현황을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "roundNo": 3,
                                "submittedCount": 4,
                                "totalCount": 6,
                                "members": [
                                  {
                                    "meetingMemberId": 10,
                                    "nickname": "startup",
                                    "bookReport": {
                                      "id": 1,
                                      "status": "APPROVED",
                                      "submittedAt": "2026-01-12T18:10:00+09:00"
                                    },
                                    "submissionRate": 70
                                  },
                                  {
                                    "meetingMemberId": 11,
                                    "nickname": "ella",
                                    "bookReport": {
                                      "id": 2,
                                      "status": "REJECTED",
                                      "submittedAt": "2026-01-12T19:00:00+09:00"
                                    },
                                    "submissionRate": 90
                                  },
                                  {
                                    "meetingMemberId": 12,
                                    "nickname": "john",
                                    "bookReport": null,
                                    "submissionRate": 50
                                  }
                                ]
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "BOOK_REPORT_MANAGEMENT_FORBIDDEN",
                              "message": "독후감 관리 권한이 없습니다."
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "ROUND_NOT_FOUND",
                              "message": "모임 회차를 찾을 수 없습니다."
                            }
                            """)
            )
    )
    ResponseEntity<ApiResult<BookReportManagementResponse>> getBookReportManagement(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "회차 ID", example = "123") Long roundId
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            summary = "모임원 독후감 상세 조회",
            description = "모임장이 모임원의 독후감 내용을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "book": {
                                  "title": "유령의 마음으로",
                                  "authors": "임선우",
                                  "publisher": "민음사",
                                  "thumbnailUrl": "https://image.kr/book/1.jpg",
                                  "publishedAt": "2022-01-01"
                                },
                                "writer": {
                                  "meetingMemberId": 11,
                                  "nickname": "ella"
                                },
                                "bookReport": {
                                  "id": 2,
                                  "status": "APPROVED",
                                  "content": "책을 읽으며 느꼈던 감정, 생각들을 자유롭게 작성해주세요!",
                                  "rejectionReason": null
                                }
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "BOOK_REPORT_MANAGEMENT_FORBIDDEN",
                              "message": "독후감 관리 권한이 없습니다."
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "회차 없음",
                                    value = """
                                            {
                                              "code": "ROUND_NOT_FOUND",
                                              "message": "모임 회차를 찾을 수 없습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "독후감 없음",
                                    value = """
                                            {
                                              "code": "BOOK_REPORT_NOT_FOUND",
                                              "message": "독후감을 찾을 수 없습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<ApiResult<MemberBookReportDetailResponse>> getMemberBookReport(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "회차 ID", example = "123") Long roundId,
            @Parameter(description = "독후감 ID", example = "456") Long bookReportId
    );
}
