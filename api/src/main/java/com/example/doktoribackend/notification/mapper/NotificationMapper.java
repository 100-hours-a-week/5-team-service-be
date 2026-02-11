package com.example.doktoribackend.notification.mapper;

import com.example.doktoribackend.notification.domain.Notification;
import com.example.doktoribackend.notification.dto.NotificationResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMapper {

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().getCode(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLinkPath(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
