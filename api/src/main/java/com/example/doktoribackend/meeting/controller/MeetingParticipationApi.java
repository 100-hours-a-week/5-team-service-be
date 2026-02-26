package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.ParticipationStatusUpdateRequest;
import com.example.doktoribackend.meeting.dto.ParticipationStatusUpdateResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MeetingParticipationApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"Meeting Participation"},
            summary = "참여 요청 승인/거절",
            description = "모임장이 참여 요청을 승인하거나 거절합니다."
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
                                "joinRequestId": 987,
                                "status": "APPROVED"
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
                                    name = "참여 요청 없음",
                                    value = """
                                            {
                                              "code": "JOIN_REQUEST_NOT_FOUND",
                                              "message": "존재하지 않는 참여 요청입니다."
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
                                    name = "이미 처리됨",
                                    value = """
                                            {
                                              "code": "JOIN_REQUEST_ALREADY_PROCESSED",
                                              "message": "이미 처리된 참여 요청입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "정원 초과",
                                    value = """
                                            {
                                              "code": "CAPACITY_FULL",
                                              "message": "모집 정원이 가득 찼습니다."
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Validation Failed",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "VALIDATION_FAILED",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                {
                                  "field": "status",
                                  "reason": "Pattern",
                                  "message": "상태는 APPROVED 또는 REJECTED만 가능합니다"
                                }
                              ]
                            }
                            """)
            )
    )
    ResponseEntity<ApiResult<ParticipationStatusUpdateResponse>> updateParticipationStatus(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId,
            @Parameter(description = "참여 요청 ID", example = "987") Long joinRequestId,
            ParticipationStatusUpdateRequest request
    );
}
