package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.MyMeetingListRequest;
import com.example.doktoribackend.meeting.dto.MyMeetingListResponse;
import com.example.doktoribackend.meeting.dto.MyMeetingDetailResponse;
import com.example.doktoribackend.meeting.service.MeetingService;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.user.dto.NotificationAgreementRequest;
import com.example.doktoribackend.user.dto.NotificationAgreementResponse;
import com.example.doktoribackend.user.dto.OnboardingRequest;
import com.example.doktoribackend.user.dto.ProfileRequiredInfoRequest;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.service.OnboardingService;
import com.example.doktoribackend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final OnboardingService onboardingService;
    private final UserService userService;
    private final MeetingService meetingService;

    @Operation(summary = "내 정보 조회", description = "로그인 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserProfileResponse response = userService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "내 정보 수정", description = "로그인 사용자의 프로필 정보를 수정합니다.")
    @PutMapping("/me")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userService.updateMyProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "프로필 필수 정보 등록", description = "성별과 출생연도 정보를 등록합니다.")
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateProfileRequiredInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileRequiredInfoRequest request
    ) {
        UserProfileResponse response = userService.updateProfileRequiredInfo(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "알림 수신 여부 변경", description = "알림 수신 동의를 설정합니다.")
    @PutMapping("/me/notifications")
    public ResponseEntity<ApiResult<Void>> updateNotificationAgreement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationAgreementRequest request
    ) {
        userService.updateNotificationAgreement(userDetails.getId(), request.notificationAgreement());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 수신 여부 조회", description = "알림 수신 동의 상태를 조회합니다.")
    @GetMapping("/me/notifications")
    public ResponseEntity<ApiResult<NotificationAgreementResponse>> getNotificationAgreement(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean agreed = userService.getNotificationAgreement(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(new NotificationAgreementResponse(agreed)));
    }

    @Operation(summary = "온보딩", description = "소셜 로그인 이후 사용자의 온보딩 정보를 저장합니다.")
    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResult<UserProfileResponse>> onboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {
        UserProfileResponse response = onboardingService.onboard(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "나의 모임 리스트 조회", description = "로그인 사용자가 참여 중인 모임 목록을 조회합니다. status는 필수이며 ACTIVE(진행 중) 또는 INACTIVE(종료)를 전달해야 합니다.")
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
                                            "meetingId": 1,
                                            "meetingImagePath": "https://image.kr/meeting/1.jpg",
                                            "title": "함께 읽는 에세이 모임",
                                            "readingGenreId": 1,
                                            "leaderNickname": "startup",
                                            "currentRound": 2,
                                            "meetingDate": "2026-01-12"
                                          },
                                          {
                                            "meetingId": 2,
                                            "meetingImagePath": "https://image.kr/meeting/2.jpg",
                                            "title": "경제/경영 스터디",
                                            "readingGenreId": 2,
                                            "leaderNickname": "ella",
                                            "currentRound": 5,
                                            "meetingDate": "2026-01-15"
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursorId": 2,
                                          "hasNext": true,
                                          "size": 10
                                        }
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
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "INVALID_STATUS_PARAMETER",
                                      "message": "status는 ACTIVE 또는 INACTIVE여야 합니다."
                                    }
                                    """)))
    })
    @GetMapping("/me/meetings")
    public ResponseEntity<ApiResult<MyMeetingListResponse>> getMyMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute MyMeetingListRequest request
    ) {
        // 인증 확인
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // status 검증 (ACTIVE or INACTIVE)
        if (!request.isValidStatus()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_PARAMETER);
        }

        // cursorId 검증
        if (request.getCursorId() != null && request.getCursorId() < 1) {
            throw new BusinessException(ErrorCode.PAGINATION_INVALID_CURSOR);
        }

        // size 검증
        if (request.getSize() != null && (request.getSize() < 1 || request.getSize() > 10)) {
            throw new BusinessException(ErrorCode.PAGINATION_SIZE_OUT_OF_RANGE);
        }

        MyMeetingListResponse response = meetingService.getMyMeetings(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "나의 오늘 모임 조회", description = "로그인 사용자의 오늘 진행되는 모임 목록을 조회합니다.")
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
                                            "meetingId": 1,
                                            "meetingImagePath": "https://image.kr/meeting/1.jpg",
                                            "title": "함께 읽는 에세이 모임",
                                            "readingGenreId": 1,
                                            "leaderNickname": "startup",
                                            "currentRound": 2,
                                            "meetingDate": "2026-01-27"
                                          }
                                        ],
                                        "pageInfo": {
                                          "nextCursorId": null,
                                          "hasNext": false,
                                          "size": 10
                                        }
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
                                    """)))
    })
    @GetMapping("/me/meetings/today")
    public ResponseEntity<ApiResult<MyMeetingListResponse>> getMyTodayMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 인증 확인
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        MyMeetingListResponse response = meetingService.getMyTodayMeetings(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "나의 모임 상세 조회", description = "로그인 사용자가 참여 중인 모임의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "AUTH_UNAUTHORIZED",
                                      "message": "인증이 필요합니다."
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Not Found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "MEETING_NOT_FOUND",
                                      "message": "존재하지 않는 모임입니다."
                                    }
                                    """)))
    })
    @GetMapping("/me/meetings/{meetingId}")
    public ResponseEntity<ApiResult<MyMeetingDetailResponse>> getMyMeetingDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long meetingId
    ) {
        // 인증 확인
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        // meetingId 검증
        if (meetingId == null || meetingId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        MyMeetingDetailResponse response = meetingService.getMyMeetingDetail(userDetails.getId(), meetingId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
