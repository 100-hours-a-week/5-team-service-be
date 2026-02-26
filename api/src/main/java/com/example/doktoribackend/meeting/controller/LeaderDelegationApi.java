package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.LeaderDelegationRequest;
import com.example.doktoribackend.meeting.dto.LeaderDelegationResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface LeaderDelegationApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Leader Delegation"},
            summary = "모임장 위임",
            description = "모임장이 다른 모임원에게 모임장 권한을 위임합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "모임장 권한이 성공적으로 위임되었습니다.",
                              "data": {
                                "meetingId": 123,
                                "leaderMeetingMemberId": 987
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
                              "code": "LEADER_DELEGATION_FORBIDDEN",
                              "message": "모임장 위임 권한이 없습니다."
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
                                    name = "미승인 멤버",
                                    value = """
                                            {
                                              "code": "DELEGATION_TARGET_NOT_APPROVED",
                                              "message": "승인된 모임원에게만 위임할 수 있습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "이미 리더",
                                    value = """
                                            {
                                              "code": "DELEGATION_TARGET_ALREADY_LEADER",
                                              "message": "이미 모임장인 멤버에게 위임할 수 없습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "자기 위임",
                                    value = """
                                            {
                                              "code": "CANNOT_DELEGATE_TO_SELF",
                                              "message": "자기 자신에게 위임할 수 없습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<ApiResult<LeaderDelegationResponse>> delegateLeader(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            LeaderDelegationRequest request
    );
}
