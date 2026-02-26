package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.exception.UserNotFoundException;
import com.example.doktoribackend.notification.domain.Notification;
import com.example.doktoribackend.notification.domain.NotificationType;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.HasUnreadResponse;
import com.example.doktoribackend.notification.dto.NotificationDeliveryTask;
import com.example.doktoribackend.notification.dto.NotificationListResponse;
import com.example.doktoribackend.notification.dto.NotificationResponse;
import com.example.doktoribackend.notification.dto.SseNotificationEvent;
import com.example.doktoribackend.notification.exception.NotificationTypeNotFoundException;
import com.example.doktoribackend.notification.mapper.NotificationMapper;
import com.example.doktoribackend.notification.repository.NotificationRepository;
import com.example.doktoribackend.notification.repository.NotificationTypeRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final UserRepository userRepository;
    private final TemplateRenderer templateRenderer;
    private final BlockingQueue<NotificationDeliveryTask> notificationDeliveryQueue;

    private static final int RECENT_DAYS = 3;

    @Transactional
    public Notification createAndSend(
            Long userId,
            NotificationTypeCode typeCode,
            Map<String, String> parameters
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        NotificationType type = notificationTypeRepository.findByCodeAndDeletedAtIsNull(typeCode)
                .orElseThrow(NotificationTypeNotFoundException::new);

        String title = type.getTitle();
        String message = templateRenderer.render(type.getMessageTemplate(), parameters);
        String linkPath = templateRenderer.render(type.getLinkTemplate(), parameters);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .linkPath(linkPath)
                .build();

        notification = notificationRepository.save(notification);

        SseNotificationEvent sseEvent = new SseNotificationEvent(
                notification.getId(),
                notification.getType().getCode(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLinkPath(),
                notification.getCreatedAt()
        );
        enqueue(new NotificationDeliveryTask(List.of(userId), title, message, linkPath, sseEvent));

        return notification;
    }

    @Transactional
    public void createAndSendBatch(
            List<Long> userIds,
            NotificationTypeCode typeCode,
            Map<String, String> parameters
    ) {
        if (userIds.isEmpty()) {
            return;
        }

        NotificationType type = notificationTypeRepository.findByCodeAndDeletedAtIsNull(typeCode)
                .orElseThrow(NotificationTypeNotFoundException::new);

        String title = type.getTitle();
        String message = templateRenderer.render(type.getMessageTemplate(), parameters);
        String linkPath = templateRenderer.render(type.getLinkTemplate(), parameters);

        List<User> users = userRepository.findAllById(userIds);

        List<Notification> notifications = users.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .type(type)
                        .title(title)
                        .message(message)
                        .linkPath(linkPath)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);

        SseNotificationEvent sseEvent = new SseNotificationEvent(
                null,
                typeCode,
                title,
                message,
                linkPath,
                LocalDateTime.now()
        );
        enqueue(new NotificationDeliveryTask(userIds, title, message, linkPath, sseEvent));
    }

    private void enqueue(NotificationDeliveryTask task) {
        if (!notificationDeliveryQueue.offer(task)) {
            log.warn("Notification delivery queue is full, task dropped for userIds: {}", task.userIds());
        }
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);

        List<Notification> notifications = notificationRepository
                .findRecentByUserId(userId, since);

        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationMapper::toResponse)
                .toList();

        return new NotificationListResponse(responses, hasUnread);
    }

    @Transactional(readOnly = true)
    public HasUnreadResponse hasUnread(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);
        boolean hasUnread = notificationRepository.existsUnreadByUserIdSince(userId, since);
        return new HasUnreadResponse(hasUnread);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);

        List<Notification> notifications = notificationRepository
                .findRecentByUserId(userId, since);

        notifications.stream()
                .filter(n -> !n.isRead())
                .forEach(Notification::markAsRead);
    }
}
