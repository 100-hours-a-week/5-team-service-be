package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.UpdateTopicsRequest;
import com.example.doktoribackend.meeting.dto.UpdateTopicsResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MeetingRoundTopicsApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting Round"},
            summary = "회차별 토론 주제 저장",
            description = "모임장이 특정 회차의 토론 주제 3개를 저장합니다. 기존 주제는 전체 덮어쓰기됩니다."
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
                                "topics": [
                                  { "topicNo": 1, "topic": "주제1", "source": "LEADER" },
                                  { "topicNo": 2, "topic": "AI 추천 주제", "source": "AI" },
                                  { "topicNo": 3, "topic": "주제3", "source": "LEADER" }
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
                              "code": "TOPIC_UPDATE_FORBIDDEN",
                              "message": "토론 주제 수정 권한이 없습니다."
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
                    examples = @ExampleObject(value = """
                            {
                              "code": "DUPLICATE_TOPIC_NO",
                              "message": "중복된 주제 번호가 있습니다."
                            }
                            """)
            )
    )
    ResponseEntity<ApiResult<UpdateTopicsResponse>> updateTopics(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "회차 ID", example = "1") Long roundId,
            UpdateTopicsRequest request
    );
}
