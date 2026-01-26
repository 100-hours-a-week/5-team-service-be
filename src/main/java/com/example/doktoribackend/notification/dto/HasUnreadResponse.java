package com.example.doktoribackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 알림 존재 여부 응답")
public record HasUnreadResponse(
        @Schema(description = "읽지 않은 알림 존재 여부")
        boolean hasUnread
) {
}
