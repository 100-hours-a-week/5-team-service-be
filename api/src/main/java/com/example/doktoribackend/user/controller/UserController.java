package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.common.util.CookieUtil;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.meeting.dto.MyMeetingListRequest;
import com.example.doktoribackend.meeting.dto.MyMeetingListResponse;
import com.example.doktoribackend.meeting.dto.MyMeetingDetailResponse;
import com.example.doktoribackend.meeting.service.MeetingService;
import com.example.doktoribackend.review.dto.MyReviewDetailResponse;
import com.example.doktoribackend.review.dto.MyReviewListResponse;
import com.example.doktoribackend.review.service.ReviewService;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.user.dto.NotificationAgreementRequest;
import com.example.doktoribackend.user.dto.NotificationAgreementResponse;
import com.example.doktoribackend.user.dto.OnboardingRequest;
import com.example.doktoribackend.user.dto.ProfileRequiredInfoRequest;
import com.example.doktoribackend.user.dto.UpdateUserProfileRequest;
import com.example.doktoribackend.user.dto.UserProfileResponse;
import com.example.doktoribackend.user.service.OnboardingService;
import com.example.doktoribackend.user.service.UserService;
import com.example.doktoribackend.meeting.dto.BookmarkedMeetingListResponse;
import com.example.doktoribackend.meeting.service.MeetingBookmarkService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserApi, UserWithdrawalApi, MyMeetingDetailApi, BookmarkedMeetingsApi {

    private final OnboardingService onboardingService;
    private final UserService userService;
    private final MeetingService meetingService;
    private final ReviewService reviewService;
    private final CookieUtil cookieUtil;
    private final MeetingBookmarkService meetingBookmarkService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserProfileResponse response = userService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PutMapping("/me")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userService.updateMyProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateProfileRequiredInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileRequiredInfoRequest request
    ) {
        UserProfileResponse response = userService.updateProfileRequiredInfo(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @PutMapping("/me/notifications")
    public ResponseEntity<ApiResult<Void>> updateNotificationAgreement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationAgreementRequest request
    ) {
        userService.updateNotificationAgreement(userDetails.getId(), request.notificationAgreement());
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/me/notifications")
    public ResponseEntity<ApiResult<NotificationAgreementResponse>> getNotificationAgreement(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean agreed = userService.getNotificationAgreement(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(new NotificationAgreementResponse(agreed)));
    }

    @Override
    @PutMapping("/me/onboarding")
    public ResponseEntity<ApiResult<UserProfileResponse>> onboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OnboardingRequest request
    ) {
        UserProfileResponse response = onboardingService.onboard(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/me/reviews")
    public ResponseEntity<ApiResult<MyReviewListResponse>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        if (size < 1 || size > 20) {
            throw new BusinessException(ErrorCode.PAGINATION_SIZE_OUT_OF_RANGE);
        }

        MyReviewListResponse response = reviewService.getMyReviews(userDetails.getId(), cursorId, size);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
    @GetMapping("/me/reviews/{reviewId}")
    public ResponseEntity<ApiResult<MyReviewDetailResponse>> getMyReviewDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        MyReviewDetailResponse response = reviewService.getMyReviewDetail(userDetails.getId(), reviewId);
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        userService.withdraw(userDetails.getId());
        cookieUtil.removeRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    private static final int MAX_BOOKMARK_SIZE = 20;

    @Override
    @GetMapping("/me/bookmarks/meetings")
    public ResponseEntity<ApiResult<BookmarkedMeetingListResponse>> getBookmarkedMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        if (cursorId != null && cursorId < 1) {
            throw new BusinessException(ErrorCode.PAGINATION_INVALID_CURSOR);
        }

        if (size < 1 || size > MAX_BOOKMARK_SIZE) {
            throw new BusinessException(ErrorCode.PAGINATION_SIZE_OUT_OF_RANGE);
        }

        BookmarkedMeetingListResponse response = meetingBookmarkService.getBookmarkedMeetings(
                userDetails.getId(), cursorId, size);
        return ResponseEntity.ok(ApiResult.ok(response));
    }
}
