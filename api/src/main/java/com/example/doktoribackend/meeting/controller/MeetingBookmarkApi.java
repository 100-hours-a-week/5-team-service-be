package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

public interface MeetingBookmarkApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "관심 모임 등록", description = "로그인 사용자가 특정 모임을 관심 모임으로 등록합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "모임 없음",
                            value = """
                                    {
                                      "code": "MEETING_NOT_FOUND",
                                      "message": "존재하지 않는 모임입니다."
                                    }
                                    """)))
    @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "이미 북마크됨",
                            value = """
                                    {
                                      "code": "BOOKMARK_ALREADY_EXISTS",
                                      "message": "이미 관심 모임으로 등록되어 있습니다."
                                    }
                                    """)))
    ResponseEntity<Void> addBookmark(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "1") Long meetingId
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "관심 모임 취소", description = "로그인 사용자가 특정 모임의 관심 모임 등록을 취소합니다. 이미 취소된 경우에도 204를 반환합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "모임 없음",
                            value = """
                                    {
                                      "code": "MEETING_NOT_FOUND",
                                      "message": "존재하지 않는 모임입니다."
                                    }
                                    """)))
    ResponseEntity<Void> removeBookmark(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "1") Long meetingId
    );
}