package com.example.doktoribackend.notification.repository;

import com.example.doktoribackend.notification.domain.NotificationType;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {

    Optional<NotificationType> findByCodeAndDeletedAtIsNull(NotificationTypeCode code);
}