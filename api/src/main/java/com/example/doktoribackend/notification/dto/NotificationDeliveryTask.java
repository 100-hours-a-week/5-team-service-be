package com.example.doktoribackend.notification.dto;

import java.util.List;

public record NotificationDeliveryTask(
        List<Long> userIds,
        String title,
        String message,
        String linkPath,
        SseNotificationEvent sseEvent
) {
}
