package com.example.doktoribackend.user.controller;

import com.example.doktoribackend.common.response.ApiResult;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final OnboardingService onboardingService;
    private final UserService userService;

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
}
