package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.MyMeetingDetailResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MyMeetingDetailApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(
            tags = {"User"},
            summary = "나의 모임 상세 조회",
            description = "로그인 사용자가 참여 중인 모임의 상세 정보를 조회합니다."
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
                                "meetingImagePath": "https://cdn.example.com/meetings/image.png",
                                "meetingImageKey": "images/meetings/36ba1999-7622-4275-b44e-9642d234b6bb.png",
                                "title": "함께 읽는 에세이 모임",
                                "readingGenreName": "에세이",
                                "leaderInfo": {
                                  "profileImagePath": "https://cdn.example.com/profiles/leader.png",
                                  "nickname": "독서리더"
                                },
                                "myRole": "MEMBER",
                                "roundCount": 4,
                                "capacity": 8,
                                "currentMemberCount": 5,
                                "currentRoundNo": 2,
                                "rounds": [
                                  {
                                    "roundId": 101,
                                    "roundNo": 1,
                                    "meetingDate": "2026-01-12T20:00:00",
                                    "dDay": -7,
                                    "meetingLink": null,
                                    "canJoinMeeting": false,
                                    "book": {
                                      "title": "아몬드",
                                      "authors": "손원평",
                                      "publisher": "창비",
                                      "thumbnailUrl": "https://cdn.example.com/books/almond.png",
                                      "publishedAt": "2017-03-31"
                                    },
                                    "bookReport": {
                                      "status": "APPROVED",
                                      "id": 501
                                    },
                                    "topics": [
                                      { "topicNo": 1, "topic": "주인공의 감정 변화에 대해 이야기해봅시다" },
                                      { "topicNo": 2, "topic": "가장 인상 깊었던 장면은?" },
                                      { "topicNo": 3, "topic": "작가가 전달하고자 한 메시지는 무엇일까요?" }
                                    ]
                                  },
                                  {
                                    "roundId": 102,
                                    "roundNo": 2,
                                    "meetingDate": "2026-01-19T20:00:00",
                                    "dDay": 0,
                                    "meetingLink": "https://zoom.us/j/123456789",
                                    "canJoinMeeting": true,
                                    "book": {
                                      "title": "다른 책",
                                      "authors": "작가명",
                                      "publisher": "출판사",
                                      "thumbnailUrl": "https://cdn.example.com/books/other.png",
                                      "publishedAt": "2020-05-15"
                                    },
                                    "bookReport": {
                                      "status": "PENDING",
                                      "id": null
                                    },
                                    "topics": []
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
                              "code": "MEETING_ACCESS_DENIED",
                              "message": "해당 모임에 접근 권한이 없습니다."
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
    ResponseEntity<ApiResult<MyMeetingDetailResponse>> getMyMeetingDetail(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "모임 ID", example = "123") Long meetingId
    );
}
