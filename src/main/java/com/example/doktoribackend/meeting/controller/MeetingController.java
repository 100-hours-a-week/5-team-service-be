package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.MeetingCreateRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateResponse;
import com.example.doktoribackend.meeting.dto.MeetingDetailResponse;
import com.example.doktoribackend.meeting.dto.JoinMeetingResponse;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingListResponse;
import com.example.doktoribackend.meeting.dto.MeetingSearchRequest;
import com.example.doktoribackend.meeting.service.MeetingService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Meeting", description = "독서 모임 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "모임 생성", description = "로그인 사용자가 모임을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "OK",
                                      "data": {
                                        "meetingId": 101
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "422", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "VALIDATION_FAILED",
                                      "message": "요청 값이 유효하지 않습니다.",
                                      "errors": [
                                        { "field": "capacity", "reason": "Min", "message": "must be greater than or equal to 3" }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AUTH_UNAUTHORIZED",
                                      "message": "인증이 필요합니다."
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<ApiResult<MeetingCreateResponse>> createMeeting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MeetingCreateRequest request
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        MeetingCreateResponse response = meetingService.createMeeting(userDetails.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getMeetingId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResult.ok(response));
    }

    @Operation(summary = "모임 리스트 조회", description = "모집 중인 모임 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "OK",
                                      "data": {
                                        "items": [
                                          {
                                            "meetingId": 101,
                                            "meetingImagePath": "https://image.kr/meeting/101.jpg",
                                            "title": "함께 읽는 에세이 모임",
                                            "readingGenreId": 1,
                                            "leaderNickname": "startup",
                                            "capacity": 8,
                                            "currentMemberCount": 5
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursorId": 149,
                                          "hasNext": true,
                                          "size": 10
                                        }
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "422", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "VALIDATION_FAILED",
                                      "message": "요청 값이 유효하지 않습니다.",
                                      "errors": [
                                        { "field": "size", "reason": "Max", "message": "must be less than or equal to 10" }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid parameter type",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "INVALID_PARAMETER_TYPE",
                                      "message": "요청 파라미터 타입이 올바르지 않습니다."
                                    }
                                    """)))
    })
    @GetMapping
    public ResponseEntity<ApiResult<MeetingListResponse>> getMeetings(
            @Valid @ModelAttribute MeetingListRequest request
    ) {
        MeetingListResponse response = meetingService.getMeetings(request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "모임 상세 조회", description = "모임의 상세 정보를 조회합니다. 인증은 선택사항입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "meeting": {
                                          "meetingId": 123,
                                          "createdAt": "2026-01-10T19:05:00+09:00",
                                          "status": "RECRUITING",
                                          "meetingImagePath": "https://cdn.example.com/meetings/...",
                                          "title": "함께 읽는 에세이 모임",
                                          "description": "매주 한 챕터씩 읽고 이야기해요.",
                                          "readingGenreId": 1,
                                          "capacity": 8,
                                          "currentCount": 5,
                                          "recruitmentDeadline": "2026-01-20",
                                          "roundCount": 2,
                                          "time": {
                                            "startTime": "20:00",
                                            "endTime": "21:30"
                                          },
                                          "leader": {
                                            "userId": 45,
                                            "nickname": "startup",
                                            "profileImagePath": "https://cdn.example.com/profiles/45.png",
                                            "intro": "안녕하세요, 함께 완독해봐요!"
                                          }
                                        },
                                        "rounds": [
                                          {
                                            "roundNo": 1,
                                            "date": "2026-01-12",
                                            "book": {
                                              "title": "아몬드",
                                              "authors": "손원평",
                                              "publisher": "출판사",
                                              "thumbnailUrl": "https://image.kr/book/1.jpg",
                                              "publishedAt": "2020-01-01"
                                            }
                                          }
                                        ],
                                        "participantsPreview": {
                                          "previewCount": 5,
                                          "profileImages": [
                                            "https://cdn.example.com/profiles/u1.png",
                                            "https://cdn.example.com/profiles/u2.png",
                                            "https://cdn.example.com/profiles/u3.png"
                                          ],
                                          "myParticipationStatus": "APPROVED"
                                        }
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Meeting not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "MEETING_NOT_FOUND",
                                      "message": "존재하지 않는 모임입니다."
                                    }
                                    """))),
            @ApiResponse(responseCode = "422", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "VALIDATION_FAILED",
                                      "message": "요청 값이 유효하지 않습니다."
                                    }
                                    """)))
    })
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResult<MeetingDetailResponse>> getMeetingDetail(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        // Validation: meetingId > 0
        if (meetingId == null || meetingId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long currentUserId = (currentUser != null) ? currentUser.getId() : null;
        MeetingDetailResponse response = meetingService.getMeetingDetail(meetingId, currentUserId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "모임 가입 요청", description = "로그인 사용자가 모임에 참여 요청을 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "OK",
                                      "message": "모임 참여 요청이 성공적으로 접수되었습니다.",
                                      "data": {
                                        "joinRequestId": 987,
                                        "meetingId": 123,
                                        "status": "PENDING",
                                        "requestedAt": "2026-01-10T21:00:00+09:00"
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AUTH_UNAUTHORIZED",
                                      "message": "인증이 필요합니다."
                                    }
                                    """))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "JOIN_REQUEST_BLOCKED",
                                      "message": "해당 모임에 참여할 수 없습니다."
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Meeting not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "MEETING_NOT_FOUND",
                                      "message": "존재하지 않는 모임입니다."
                                    }
                                    """))),
            @ApiResponse(responseCode = "409", description = "Conflict",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "JOIN_REQUEST_ALREADY_EXISTS",
                                      "message": "이미 참여 요청이 접수된 모임입니다."
                                    }
                                    """)))
    })
    @PostMapping("/{meetingId}/participations")
    public ResponseEntity<ApiResult<JoinMeetingResponse>> joinMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 인증 확인
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // Validation: meetingId > 0
        if (meetingId == null || meetingId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        JoinMeetingResponse response = meetingService.joinMeeting(userDetails.getId(), meetingId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getJoinRequestId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResult.ok(response));
    }

    @Operation(summary = "모임 검색", description = "책 제목 또는 모임 제목으로 모임을 검색합니다. 책 제목 매칭이 우선이며, RECRUITING 상태가 먼저 표시됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "OK",
                                      "message": "요청이 성공적으로 처리되었습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "meetingId": 101,
                                            "meetingImagePath": "https://cdn.example.com/meetings/101.jpg",
                                            "title": "함께 읽는 에세이 모임",
                                            "readingGenreId": 1,
                                            "leaderNickname": "startup",
                                            "capacity": 8,
                                            "currentMemberCount": 5,
                                            "remainingDays": 4
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursorId": 150,
                                          "hasNext": true,
                                          "size": 10
                                        }
                                      }
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "INVALID_REQUEST",
                                      "message": "올바르지 않은 요청입니다."
                                    }
                                    """)))
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResult<MeetingListResponse>> searchMeetings(
            @Valid @ParameterObject MeetingSearchRequest request
    ) {
        MeetingListResponse response = meetingService.searchMeetings(request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
