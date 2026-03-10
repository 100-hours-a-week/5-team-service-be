package com.example.doktoribackend.review.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.review.dto.ReviewCreateRequest;
import com.example.doktoribackend.review.dto.ReviewCreateResponse;
import com.example.doktoribackend.review.dto.ReviewListResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Review", description = "리뷰 API")
public interface ReviewApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            summary = "리뷰 생성",
            description = "모임 회차 종료 후 24시간 이내에 리뷰를 작성합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "reviewId": 1
                              }
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_BEST_MEMBER",
                              "message": "유효하지 않은 베스트 모임원입니다."
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
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "이미 제출",
                                    value = """
                                            {
                                              "code": "REVIEW_ALREADY_SUBMITTED",
                                              "message": "이미 리뷰를 작성했습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "회차 미종료",
                                    value = """
                                            {
                                              "code": "ROUND_NOT_COMPLETED",
                                              "message": "아직 종료되지 않은 회차입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "기간 만료",
                                    value = """
                                            {
                                              "code": "REVIEW_PERIOD_EXPIRED",
                                              "message": "리뷰 작성 기간이 만료되었습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<ApiResult<ReviewCreateResponse>> createReview(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 회차 ID", example = "1") Long meetingRoundId,
            ReviewCreateRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            summary = "리뷰 삭제",
            description = "자신이 작성한 리뷰를 삭제합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "No Content"
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "REVIEW_DELETE_FORBIDDEN",
                              "message": "리뷰 삭제 권한이 없습니다."
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
                              "code": "REVIEW_NOT_FOUND",
                              "message": "리뷰를 찾을 수 없습니다."
                            }
                            """)
            )
    )
    ResponseEntity<Void> deleteReview(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            Long reviewId
    );

    @CommonErrorResponses
    @Operation(
            summary = "모임장 리뷰 목록 조회",
            description = "모임 상세 페이지에서 해당 모임장이 만든 모든 모임들의 리뷰를 조회합니다. 비인증 API입니다."
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
                                "items": [
                                  {
                                    "reviewId": 10,
                                    "reviewerProfileImageUrl": "https://image.kr/profiles/1.jpg",
                                    "meetingTitle": "함께 읽는 에세이 모임",
                                    "roundNo": 2,
                                    "bookTitle": "데미안",
                                    "meetingRating": 4.5,
                                    "content": "좋은 모임이었습니다.",
                                    "imageUrls": ["https://image.kr/reviews/1.jpg"]
                                  }
                                ],
                                "pageInfo": {
                                  "nextCursorId": 9,
                                  "hasNext": true,
                                  "size": 10
                                }
                              }
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
                              "code": "MEETING_NOT_FOUND",
                              "message": "존재하지 않는 모임입니다."
                            }
                            """)
            )
    )
    ResponseEntity<ApiResult<ReviewListResponse>> getLeaderReviews(
            Long meetingId,
            Long cursorId,
            Integer size
    );
}
