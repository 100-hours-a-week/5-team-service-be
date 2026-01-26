package com.example.doktoribackend.notification.dto;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID")
        Long id,

        @Schema(description = "알림 타입 코드")
        NotificationTypeCode typeCode,

        @Schema(description = "알림 제목")
        String title,

        @Schema(description = "알림 메시지")
        String message,

        @Schema(description = "이동 경로")
        String linkPath,

        @Schema(description = "읽음 여부")
        boolean isRead,

        @Schema(description = "생성일시")
        LocalDateTime createdAt
) {
}
