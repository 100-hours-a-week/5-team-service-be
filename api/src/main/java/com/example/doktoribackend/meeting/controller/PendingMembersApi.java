package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.PendingMembersResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PendingMembersApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting"},
            summary = "가입 신청 대기 멤버 조회",
            description = "모임장이 해당 모임에 가입 신청한 PENDING 상태의 멤버 목록을 조회합니다. 최신순 정렬, 커서 기반 페이지네이션을 지원합니다."
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
                                "members": [
                                  {
                                    "meetingMemberId": 15,
                                    "nickname": "readerC",
                                    "memberIntro": "독서를 좋아하는 직장인입니다.",
                                    "profileImagePath": "https://cdn.example.com/profiles/15.png"
                                  },
                                  {
                                    "meetingMemberId": 14,
                                    "nickname": "readerB",
                                    "memberIntro": "매일 한 권씩 읽고 있습니다.",
                                    "profileImagePath": "https://cdn.example.com/profiles/14.png"
                                  }
                                ],
                                "pageInfo": {
                                  "nextCursorId": 14,
                                  "hasNext": true,
                                  "size": 20
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
    ResponseEntity<ApiResult<PendingMembersResponse>> getPendingMembers(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            @Parameter(description = "커서 ID (이전 페이지 마지막 meetingMemberId)") Long cursorId,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 20)", example = "20") Integer size
    );
}
