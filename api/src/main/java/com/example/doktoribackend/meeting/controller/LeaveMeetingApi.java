package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface LeaveMeetingApi {

    @AuthErrorResponses
    @Operation(
            tags = {"Leave Meeting"},
            summary = "모임 탈퇴",
            description = "본인이 해당 모임에서 탈퇴합니다. APPROVED 상태인 멤버만 탈퇴 가능하며, 모임장은 위임 후 탈퇴해야 합니다."
    )
    @ApiResponse(
            responseCode = "204",
            description = "No Content - 탈퇴 성공"
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
                                    name = "멤버 아님",
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
                                    name = "모임장 탈퇴 불가",
                                    value = """
                                            {
                                              "code": "LEADER_CANNOT_LEAVE",
                                              "message": "모임장은 위임 후 탈퇴해야 합니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "탈퇴 불가 상태",
                                    value = """
                                            {
                                              "code": "LEAVE_NOT_ALLOWED",
                                              "message": "탈퇴할 수 없는 상태입니다."
                                            }
                                            """
                            )
                    }
            )
    )
    ResponseEntity<Void> leaveMeeting(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId
    );
}
