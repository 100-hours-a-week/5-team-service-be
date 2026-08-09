package com.example.doktoribackend.notification.dto;

import java.util.List;

public record NotificationDeliveryTask(
        List<Long> userIds,
        String title,
        String message,
        String linkPath,
        SseNotificationEvent sseEvent
) {

    /**
     * 재시도 대상만 남긴 사본. 전체를 재발행하면 이미 전달에 성공한 사용자에게 중복 푸시가 간다.
     */
    public NotificationDeliveryTask withUserIds(List<Long> retryUserIds) {
        return new NotificationDeliveryTask(List.copyOf(retryUserIds), title, message, linkPath, sseEvent);
    }
}
