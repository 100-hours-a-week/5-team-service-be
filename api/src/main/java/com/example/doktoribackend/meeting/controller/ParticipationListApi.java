package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.ParticipationListResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface ParticipationListApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting"},
            summary = "참여 요청 대기 목록 조회",
            description = "모임장이 해당 모임의 참여 요청 대기 목록을 조회합니다."
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
                                "items": [
                                  {
                                    "meetingMemberId": 11,
                                    "status": "PENDING",
                                    "requestedAt": "2026-01-10T21:00:00+09:00",
                                    "nickname": "readerA",
                                    "memberIntro": "열심히 참여하는 모임원이 되겠습니다!",
                                    "profileImagePath": "https://cdn.example.com/profiles/11.png"
                                  },
                                  {
                                    "meetingMemberId": 12,
                                    "status": "PENDING",
                                    "requestedAt": "2026-01-10T21:03:00+09:00",
                                    "nickname": "readerB",
                                    "memberIntro": "열심히 참여하는 모임원이 되겠습니다!",
                                    "profileImagePath": "https://cdn.example.com/profiles/12.png"
                                  }
                                ],
                                "pageInfo": {
                                  "nextCursorId": 149,
                                  "hasNext": true,
                                  "size": 10
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
                              "code": "AUTH_FORBIDDEN",
                              "message": "접근 권한이 없습니다."
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
    ResponseEntity<ApiResult<ParticipationListResponse>> getParticipations(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            @Parameter(description = "페이지 크기 (기본값 10)", example = "10") Integer size,
            @Parameter(description = "커서 ID (meetingMemberId)", example = "150") Long cursorId
    );
}
