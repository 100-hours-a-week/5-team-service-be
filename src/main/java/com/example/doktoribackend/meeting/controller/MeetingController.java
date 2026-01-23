package com.example.doktoribackend.meeting.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.MeetingCreateRequest;
import com.example.doktoribackend.meeting.dto.MeetingCreateResponse;
import com.example.doktoribackend.meeting.dto.MeetingListRequest;
import com.example.doktoribackend.meeting.dto.MeetingListResponse;
import com.example.doktoribackend.meeting.service.MeetingService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
}
