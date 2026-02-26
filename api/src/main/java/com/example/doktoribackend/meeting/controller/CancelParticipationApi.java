package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface CancelParticipationApi {

    @AuthErrorResponses
    @Operation(
            tags = {"Cancel Participation"},
            summary = "참여 요청 취소",
            description = "본인의 모임 참여 요청(PENDING 상태)을 취소합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "No Content - 취소 성공"
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
                    examples = @ExampleObject(value = """
                            {
                              "code": "PARTICIPATION_CANCEL_NOT_ALLOWED",
                              "message": "이미 승인된 참여 요청은 취소할 수 없습니다."
                            }
                            """)
            )
    )
    ResponseEntity<Void> cancelParticipation(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId
    );
}
