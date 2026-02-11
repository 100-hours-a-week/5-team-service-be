package com.example.doktoribackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 목록 응답")
public record NotificationListResponse(
        @Schema(description = "알림 목록")
        List<NotificationResponse> notifications,

        @Schema(description = "읽지 않은 알림 존재 여부")
        boolean hasUnread
) {
}
