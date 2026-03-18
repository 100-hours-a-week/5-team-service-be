package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.MeetingBookmarkStatusResponse;
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

    @Operation(summary = "관심 모임 여부 조회", description = "로그인 사용자의 해당 모임 북마크 여부를 조회합니다. 비로그인 시 null을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "로그인 + 북마크됨", value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "isBookmarked": true
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "로그인 + 북마크 안됨", value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "isBookmarked": false
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "비로그인", value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "isBookmarked": null
                                      }
                                    }
                                    """)
                    }))
    @ApiResponse(responseCode = "404", description = "Meeting not found",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_NOT_FOUND",
                              "message": "존재하지 않는 모임입니다."
                            }
                            """)))
    ResponseEntity<ApiResult<MeetingBookmarkStatusResponse>> getBookmarkStatus(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId
    );
}