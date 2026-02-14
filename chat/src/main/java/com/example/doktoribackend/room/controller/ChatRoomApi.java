package com.example.doktoribackend.room.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "ChatRoom", description = "채팅방 API")
public interface ChatRoomApi {

    @CommonErrorResponses
    @Operation(summary = "채팅방 목록 조회", description = "커서 기반 페이지네이션으로 채팅방 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "items": [
                                  {
                                    "roomId": 1,
                                    "topic": "AI가 인간의 일자리를 대체할 수 있는가",
                                    "description": "AI 기술 발전에 따른 고용 시장 변화를 토론합니다",
                                    "capacity": 4,
                                    "currentMemberCount": 2
                                  }
                                ],
                                "pageInfo": {
                                  "nextCursorId": 1,
                                  "hasNext": false,
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
                                              "message": "size는 1~10 사이여야 합니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<ChatRoomListResponse>> getChatRooms(
            @Parameter(description = "마지막으로 조회한 채팅방 ID (첫 조회 시 생략)", example = "10")
            Long cursorId,
            @Parameter(description = "조회할 채팅방 수 (기본값: 10, 최대: 20)", example = "10")
            Integer size);

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "채팅방 생성", description = "새로운 토론 채팅방을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "roomId": 1
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "422", description = "Validation Failed",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "VALIDATION_FAILED",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                {
                                  "field": "topic",
                                  "reason": "NotBlank",
                                  "message": "주제는 필수입니다."
                                },
                                {
                                  "field": "capacity",
                                  "reason": "NotNull",
                                  "message": "정원은 필수입니다."
                                },
                                {
                                  "field": "quiz.question",
                                  "reason": "Size",
                                  "message": "퀴즈 질문은 2~50자 사이여야 합니다."
                                }
                              ]
                            }
                            """)))
    ResponseEntity<ApiResult<ChatRoomCreateResponse>> createChatRoom(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            ChatRoomCreateRequest request);
}
