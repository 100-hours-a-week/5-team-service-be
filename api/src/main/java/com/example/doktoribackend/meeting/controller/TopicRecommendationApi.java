package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.TopicRecommendationResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface TopicRecommendationApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting"},
            summary = "토론 주제 AI 추천",
            description = "모임장이 특정 회차의 토론 주제를 AI로 추천받습니다."
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
                                "meetingId": 123,
                                "roundNo": 1,
                                "topic": "주인공의 주요 선택들이 관계와 가치관을 어떻게 바꿨는지 토론합니다.",
                                "remainingCount": 14
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
                              "code": "TOPIC_RECOMMENDATION_FORBIDDEN",
                              "message": "토론 주제 추천 권한이 없습니다."
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
                                    name = "모임 없음",
                                    value = """
                                            {
                                              "code": "MEETING_NOT_FOUND",
                                              "message": "존재하지 않는 모임입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "회차 없음",
                                    value = """
                                            {
                                              "code": "ROUND_NOT_FOUND",
                                              "message": "모임 회차를 찾을 수 없습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "독후감 없음",
                                    value = """
                                            {
                                              "code": "NO_BOOK_REPORTS_FOR_TOPIC",
                                              "message": "제출된 독후감이 없어 AI 추천을 할 수 없습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "일일 제한 초과",
                                    value = """
                                            {
                                              "code": "TOPIC_RECOMMENDATION_LIMIT_EXCEEDED",
                                              "message": "일일 AI 추천 횟수(15회)를 초과했습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<ApiResult<TopicRecommendationResponse>> recommendTopic(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            @Parameter(description = "회차 번호", example = "1") Integer roundNo
    );
}
