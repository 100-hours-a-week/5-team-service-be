package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.MeetingMembersResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MeetingMembersApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting"},
            summary = "가입된 멤버 조회",
            description = "모임장이 해당 모임에 가입된 멤버 목록을 조회합니다. 모임장 본인도 포함됩니다."
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
                                "memberCount": 2,
                                "members": [
                                  {
                                    "meetingMemberId": 11,
                                    "nickname": "readerA",
                                    "profileImagePath": "https://cdn.example.com/profiles/11.png",
                                    "joinedAt": "2026-01-10T20:00:00+09:00"
                                  },
                                  {
                                    "meetingMemberId": 12,
                                    "nickname": "readerB",
                                    "profileImagePath": "https://cdn.example.com/profiles/12.png",
                                    "joinedAt": "2026-01-10T20:05:00+09:00"
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
    ResponseEntity<ApiResult<MeetingMembersResponse>> getMeetingMembers(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId
    );
}
