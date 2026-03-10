package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.BookmarkedMeetingListResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

public interface BookmarkedMeetingsApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "관심 모임 리스트 조회", description = "로그인 사용자가 북마크한 모임 목록을 조회합니다. 북마크 최신순으로 정렬됩니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "data": [
                                  {
                                    "meetingId": 149,
                                    "meetingImagePath": "https://image.kr/meeting/149.jpg",
                                    "title": "함께 읽는 에세이 모임",
                                    "readingGenreName": "인문",
                                    "leaderNickname": "startup",
                                    "currentMemberCount": 2,
                                    "capacity": 6,
                                    "isRecruiting": true
                                  }
                                ],
                                "pageInfo": {
                                  "nextCursorId": 149,
                                  "hasNext": true,
                                  "size": 10
                                }
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "잘못된 cursorId",
                                    value = """
                                            {
                                              "code": "PAGINATION_INVALID_CURSOR",
                                              "message": "cursorId는 1 이상의 정수여야 합니다."
                                            }
                                            """),
                            @ExampleObject(name = "잘못된 size",
                                    value = """
                                            {
                                              "code": "PAGINATION_SIZE_OUT_OF_RANGE",
                                              "message": "size는 1~20 사이여야 합니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<BookmarkedMeetingListResponse>> getBookmarkedMeetings(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "마지막으로 조회한 북마크 ID (첫 조회 시 생략)", example = "150") Long cursorId,
            @Parameter(description = "조회할 개수 (기본값: 10, 최대: 20)", example = "10") Integer size
    );
}