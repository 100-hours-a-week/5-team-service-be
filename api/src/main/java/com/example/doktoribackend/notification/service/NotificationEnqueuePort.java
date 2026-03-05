package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;

public interface NotificationEnqueuePort {
    void enqueue(NotificationDeliveryTask task);
}
