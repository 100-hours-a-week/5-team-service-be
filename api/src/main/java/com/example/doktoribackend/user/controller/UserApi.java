package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.swagger.AuthErrorResponses;
import com.example.doktoribackend.common.swagger.CommonErrorResponses;
import com.example.doktoribackend.meeting.dto.MyMeetingListResponse;
import com.example.doktoribackend.review.dto.MyReviewDetailResponse;
import com.example.doktoribackend.review.dto.MyReviewListResponse;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.user.dto.NotificationAgreementRequest;
import com.example.doktoribackend.user.dto.NotificationAgreementResponse;
import com.example.doktoribackend.user.dto.OnboardingRequest;
import com.example.doktoribackend.user.dto.ProfileRequiredInfoRequest;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "내 정보 조회", description = "로그인 사용자의 프로필 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "독서왕",
                                "profileImageUrl": "https://image.kr/profiles/1.jpg",
                                "gender": "MALE",
                                "birthYear": 1995
                              }
                            }
                            """)))
    ResponseEntity<ApiResult<UserProfileResponse>> getMyProfile(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "내 정보 수정", description = "로그인 사용자의 프로필 정보를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "독서왕",
                                "profileImageUrl": "https://image.kr/profiles/1.jpg",
                                "gender": "MALE",
                                "birthYear": 1995
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_INPUT_VALUE",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                { "field": "nickname", "reason": "NOT_BLANK", "message": "닉네임은 필수입니다" },
                                { "field": "nickname", "reason": "LENGTH", "message": "닉네임은 20자를 초과할 수 없습니다" },
                                { "field": "profileImagePath", "reason": "LENGTH", "message": "프로필 이미지 경로는 512자를 초과할 수 없습니다" },
                                { "field": "leaderIntro", "reason": "LENGTH", "message": "소개는 300자를 초과할 수 없습니다" }
                              ]
                            }
                            """)))
    ResponseEntity<ApiResult<UserProfileResponse>> updateMyProfile(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            UpdateUserProfileRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "프로필 필수 정보 등록", description = "성별과 출생연도 정보를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "독서왕",
                                "profileImageUrl": "https://image.kr/profiles/1.jpg",
                                "gender": "MALE",
                                "birthYear": 1995
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_INPUT_VALUE",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                { "field": "gender", "reason": "NOT_NULL", "message": "성별은 필수입니다." },
                                { "field": "birthYear", "reason": "NOT_NULL", "message": "생년월일은 필수입니다." },
                                { "field": "birthYear", "reason": "RANGE", "message": "출생년도는 1900 이상이어야 합니다." },
                                { "field": "notificationAgreement", "reason": "NOT_NULL", "message": "알림 수신 여부는 필수입니다." }
                              ]
                            }
                            """)))
    ResponseEntity<ApiResult<UserProfileResponse>> updateProfileRequiredInfo(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            ProfileRequiredInfoRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "알림 수신 여부 변경", description = "알림 수신 동의를 설정합니다.")
    @ApiResponse(responseCode = "204", description = "No Content")
    @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_INPUT_VALUE",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                { "field": "notificationAgreement", "reason": "NOT_NULL", "message": "알림 수신 여부는 필수입니다." }
                              ]
                            }
                            """)))
    ResponseEntity<ApiResult<Void>> updateNotificationAgreement(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            NotificationAgreementRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "알림 수신 여부 조회", description = "알림 수신 동의 상태를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "notificationAgreement": true
                              }
                            }
                            """)))
    ResponseEntity<ApiResult<NotificationAgreementResponse>> getNotificationAgreement(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "온보딩", description = "소셜 로그인 이후 사용자의 온보딩 정보를 저장합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "독서왕",
                                "profileImageUrl": "https://image.kr/profiles/1.jpg",
                                "gender": "MALE",
                                "birthYear": 1995
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "422", description = "Unprocessable Entity",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "INVALID_INPUT_VALUE",
                              "message": "요청 값이 유효하지 않습니다.",
                              "errors": [
                                { "field": "readingVolumeId", "reason": "POSITIVE", "message": "0보다 커야 합니다" },
                                { "field": "readingPurposeIds", "reason": "LENGTH", "message": "독서 목적은 최대 3개 선택 가능합니다" },
                                { "field": "readingGenreIds", "reason": "LENGTH", "message": "선호 장르는 최대 2개 선택 가능합니다" }
                              ]
                            }
                            """)))
    ResponseEntity<ApiResult<UserProfileResponse>> onboard(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            OnboardingRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "나의 리뷰 목록 조회", description = "내가 작성한 리뷰 목록을 커서 기반 페이지네이션으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "items": [
                                  {
                                    "reviewId": 1,
                                    "meetingTitle": "함께 읽는 에세이 모임",
                                    "roundNo": 2,
                                    "bookTitle": "데미안",
                                    "meetingRating": 4.5,
                                    "content": "좋은 모임이었습니다.",
                                    "imageUrls": ["https://image.kr/reviews/1.jpg"]
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
                    examples = @ExampleObject(value = """
                            {
                              "code": "PAGINATION_SIZE_OUT_OF_RANGE",
                              "message": "size는 1~10 사이여야 합니다."
                            }
                            """)))
    ResponseEntity<ApiResult<MyReviewListResponse>> getMyReviews(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "마지막으로 조회한 리뷰 ID (첫 조회 시 생략)", example = "10") Long cursorId,
            @Parameter(description = "조회할 개수 (기본값: 10, 최대: 20)", example = "10") Integer size
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "나의 리뷰 상세 조회", description = "내가 작성한 리뷰의 상세 정보를 조회합니다. 본인이 작성한 리뷰만 조회 가능합니다.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "reviewId": 1,
                                "meetingTitle": "함께 읽는 에세이 모임",
                                "roundNo": 2,
                                "bookTitle": "데미안",
                                "meetingRating": 4.5,
                                "leaderRating": 4.0,
                                "content": "좋은 모임이었습니다.",
                                "bestMemberId": 2,
                                "imageUrls": ["https://image.kr/reviews/1.jpg"],
                                "members": [
                                  {
                                    "userId": 2,
                                    "nickname": "독서왕",
                                    "profileImageUrl": "https://image.kr/profiles/2.jpg"
                                  }
                                ]
                              }
                            }
                            """)))
    @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "code": "REVIEW_NOT_FOUND",
                              "message": "리뷰를 찾을 수 없습니다."
                            }
                            """)))
    ResponseEntity<ApiResult<MyReviewDetailResponse>> getMyReviewDetail(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            @Parameter(description = "리뷰 ID", example = "1") Long reviewId
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "나의 모임 리스트 조회", description = "로그인 사용자가 참여 중인 모임 목록을 조회합니다. status는 필수이며 ACTIVE(진행 중) 또는 INACTIVE(종료) 또는 PENDING(승인 대기)을 전달해야 합니다.")
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
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "code": "INVALID_STATUS_PARAMETER",
                                      "message": "status는 ACTIVE 또는 INACTIVE여야 합니다."
                                    }
                                    """)))
    })
    ResponseEntity<ApiResult<MyMeetingListResponse>> getMyMeetings(
            @Parameter(hidden = true) CustomUserDetails userDetails,
            com.example.doktoribackend.meeting.dto.MyMeetingListRequest request
    );

    @CommonErrorResponses
    @AuthErrorResponses
    @Operation(summary = "나의 오늘 모임 조회", description = "로그인 사용자의 오늘 진행되는 모임 목록을 조회합니다.")
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
                            """)))
    ResponseEntity<ApiResult<MyMeetingListResponse>> getMyTodayMeetings(
            @Parameter(hidden = true) CustomUserDetails userDetails
    );
}
