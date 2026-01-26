package com.example.doktoribackend.notification.dto;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;

import java.time.LocalDateTime;

public record SseNotificationEvent(
        Long id,
        NotificationTypeCode typeCode,
        String title,
        String message,
        String linkPath,
        LocalDateTime createdAt
) {
}
