package com.example.doktoribackend.room.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.room.dto.ChatRoomCreateRequest;
import com.example.doktoribackend.room.dto.ChatRoomCreateResponse;
import com.example.doktoribackend.room.dto.ChatRoomJoinRequest;
import com.example.doktoribackend.room.dto.ChatRoomListResponse;
import com.example.doktoribackend.room.dto.WaitingRoomResponse;
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
                                    "currentMemberCount": 2,
                                    "bookTitle": "아몬드",
                                    "bookAuthors": "손원평",
                                    "bookThumbnailUrl": "https://example.com/thumb.jpg"
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
                                  "field": "isbn",
                                  "reason": "NotBlank",
                                  "message": "ISBN은 필수입니다."
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

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "채팅방 나가기", description = "현재 참여 중인 채팅방에서 나갑니다. 대기 중인 방의 방장이 나가면 방이 취소됩니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "채팅방 없음",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_NOT_FOUND",
                                              "message": "존재하지 않는 채팅방입니다."
                                            }
                                            """),
                            @ExampleObject(name = "멤버 아님",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_MEMBER_NOT_FOUND",
                                              "message": "채팅방 멤버가 아닙니다."
                                            }
                                            """)
                    }))
    @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "이미 종료된 채팅방",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_ALREADY_ENDED",
                                              "message": "이미 종료되거나 취소된 채팅방입니다."
                                            }
                                            """),
                            @ExampleObject(name = "이미 나간 채팅방",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_ALREADY_LEFT",
                                              "message": "이미 나간 채팅방입니다."
                                            }
                                            """)
                    }))
    ResponseEntity<Void> leaveChatRoom(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", example = "1") Long roomId);

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "채팅방 참여", description = "퀴즈를 맞추고 채팅방에 참여합니다. 포지션(찬성/반대)을 선택해야 합니다.")
    @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "OK",
                              "data": {
                                "roomId": 1,
                                "agreeCount": 2,
                                "disagreeCount": 1,
                                "maxPerPosition": 3,
                                "members": [
                                  {
                                    "nickname": "독서왕",
                                    "profileImageUrl": "https://example.com/profile.jpg",
                                    "position": "AGREE",
                                    "role": "HOST"
                                  },
                                  {
                                    "nickname": "책벌레",
                                    "profileImageUrl": null,
                                    "position": "DISAGREE",
                                    "role": "PARTICIPANT"
                                  }
                                ]
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "퀴즈 오답",
                            value = """
                                    {
                                      "code": "CHAT_ROOM_QUIZ_WRONG_ANSWER",
                                      "message": "퀴즈 정답이 아닙니다."
                                    }
                                    """)))
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "채팅방 없음",
                            value = """
                                    {
                                      "code": "CHAT_ROOM_NOT_FOUND",
                                      "message": "존재하지 않는 채팅방입니다."
                                    }
                                    """)))
    @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(mediaType = "application/json",
                    examples = {
                            @ExampleObject(name = "이미 참여 중",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_ALREADY_JOINED",
                                              "message": "이미 참여 중인 채팅방이 있어 새 채팅방을 생성할 수 없습니다."
                                            }
                                            """),
                            @ExampleObject(name = "정원 초과",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_FULL",
                                              "message": "채팅방 정원이 가득 찼습니다."
                                            }
                                            """),
                            @ExampleObject(name = "포지션 정원 초과",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_POSITION_FULL",
                                              "message": "해당 포지션의 정원이 가득 찼습니다."
                                            }
                                            """),
                            @ExampleObject(name = "대기 중이 아닌 채팅방",
                                    value = """
                                            {
                                              "code": "CHAT_ROOM_NOT_WAITING",
                                              "message": "대기 중인 채팅방만 참여할 수 있습니다."
                                            }
                                            """)
                    }))
    ResponseEntity<ApiResult<WaitingRoomResponse>> joinChatRoom(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "채팅방 ID", example = "1") Long roomId,
            ChatRoomJoinRequest request);
}
