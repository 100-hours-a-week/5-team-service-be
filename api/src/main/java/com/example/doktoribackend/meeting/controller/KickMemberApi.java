package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface KickMemberApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting"},
            summary = "모임원 강퇴",
            description = "모임장이 특정 모임원을 강퇴합니다."
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
                                    name = "멤버 없음",
                                    value = """
                                            {
                                              "code": "MEETING_MEMBER_NOT_FOUND",
                                              "message": "존재하지 않는 모임 멤버입니다."
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
                                    name = "본인 강퇴 시도",
                                    value = """
                                            {
                                              "code": "CANNOT_KICK_SELF",
                                              "message": "자기 자신을 강퇴할 수 없습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "강퇴 불가 상태",
                                    value = """
                                            {
                                              "code": "KICK_NOT_ALLOWED",
                                              "message": "강퇴할 수 없는 상태입니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<Void> kickMember(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            @Parameter(description = "멤버 ID (meetingMemberId)", example = "456") Long memberId
    );
}
