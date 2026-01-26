package com.example.doktoribackend.notification.controller;

import com.example.doktoribackend.common.response.ApiResult;
import com.example.doktoribackend.notification.dto.HasUnreadResponse;
import com.example.doktoribackend.notification.dto.NotificationListResponse;
import com.example.doktoribackend.notification.dto.PushTokenRequest;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.notification.service.PushTokenService;
import com.example.doktoribackend.notification.service.SseEmitterService;
import com.example.doktoribackend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final PushTokenService pushTokenService;
    private final SseEmitterService sseEmitterService;

    @Operation(summary = "알림 목록 조회", description = "최근 3일간의 알림 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResult<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        NotificationListResponse response = notificationService.getNotifications(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "읽지 않은 알림 존재 여부", description = "최근 3일 중 읽지 않은 알림이 있는지 확인합니다.")
    @GetMapping("/has-unread")
    public ResponseEntity<ApiResult<HasUnreadResponse>> hasUnread(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        HasUnreadResponse response = notificationService.hasUnread(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음으로 표시합니다.")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResult<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userDetails.getId(), notificationId);
        return ResponseEntity.ok(ApiResult.ok());
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "최근 3일간의 모든 알림을 읽음으로 표시합니다.")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResult<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(ApiResult.ok());
    }

    @Operation(summary = "FCM 푸시 토큰 등록", description = "Firebase Cloud Messaging 푸시 토큰을 등록합니다.")
    @PostMapping("/push-token")
    public ResponseEntity<ApiResult<Void>> registerPushToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PushTokenRequest request
    ) {
        pushTokenService.registerToken(
                userDetails.getId(),
                request.token(),
                request.platform()
        );
        return ResponseEntity.ok(ApiResult.ok());
    }

    @Operation(summary = "실시간 알림 구독 (SSE)",
            description = "Server-Sent Events를 통해 실시간 알림을 수신합니다. "
                    + "연결 후 'notification' 이벤트로 새 알림이 전송됩니다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return sseEmitterService.subscribe(userDetails.getId());
    }
}
