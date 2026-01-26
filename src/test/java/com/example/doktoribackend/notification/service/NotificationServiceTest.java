package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.notification.domain.Notification;
import com.example.doktoribackend.notification.domain.NotificationType;
import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.dto.HasUnreadResponse;
import com.example.doktoribackend.notification.dto.NotificationListResponse;
import com.example.doktoribackend.notification.exception.NotificationTypeNotFoundException;
import com.example.doktoribackend.notification.repository.NotificationRepository;
import com.example.doktoribackend.notification.repository.NotificationTypeRepository;
import com.example.doktoribackend.user.domain.User;
import com.example.doktoribackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    NotificationTypeRepository notificationTypeRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    FcmService fcmService;

    @Mock
    SseEmitterService sseEmitterService;

    @Mock
    TemplateRenderer templateRenderer;

    @InjectMocks
    NotificationService notificationService;

    @Test
    @DisplayName("createAndSend: 알림을 생성하고 FCM, SSE로 전송한다")
    void createAndSend_success() {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.ROUND_START_10M_BEFORE)
                .title("10분 후 토론이 시작돼요")
                .messageTemplate("곧 화상 토론이 열려요.")
                .linkTemplate("/users/me/meetings/{meetingId}")
                .build();
        ReflectionTestUtils.setField(type, "id", 1L);

        Notification savedNotification = Notification.builder()
                .user(user)
                .type(type)
                .title("10분 후 토론이 시작돼요")
                .message("곧 화상 토론이 열려요.")
                .linkPath("/users/me/meetings/123")
                .build();
        ReflectionTestUtils.setField(savedNotification, "id", 100L);
        ReflectionTestUtils.setField(savedNotification, "createdAt", LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(notificationTypeRepository.findByCodeAndDeletedAtIsNull(NotificationTypeCode.ROUND_START_10M_BEFORE))
                .willReturn(Optional.of(type));
        given(templateRenderer.render(eq("곧 화상 토론이 열려요."), any())).willReturn("곧 화상 토론이 열려요.");
        given(templateRenderer.render(eq("/users/me/meetings/{meetingId}"), any())).willReturn("/users/me/meetings/123");
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // when
        Notification result = notificationService.createAndSend(
                userId,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                Map.of("meetingId", "123")
        );

        // then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTitle()).isEqualTo("10분 후 토론이 시작돼요");

        then(notificationRepository).should().save(any(Notification.class));
        then(sseEmitterService).should().sendToUser(eq(userId), any());
        then(fcmService).should().sendToUser(eq(userId), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createAndSend: 사용자가 없으면 예외가 발생한다")
    void createAndSend_userNotFound_throws() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.createAndSend(
                99L,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                Map.of()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createAndSend: 알림 타입이 없으면 예외가 발생한다")
    void createAndSend_typeNotFound_throws() {
        // given
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationTypeRepository.findByCodeAndDeletedAtIsNull(any()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.createAndSend(
                1L,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                Map.of()
        ))
                .isInstanceOf(NotificationTypeNotFoundException.class);

        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createAndSendBatch: 여러 사용자에게 알림을 일괄 전송한다")
    void createAndSendBatch_success() {
        // given
        List<Long> userIds = List.of(1L, 2L, 3L);

        User user1 = User.builder().nickname("user1").build();
        User user2 = User.builder().nickname("user2").build();
        User user3 = User.builder().nickname("user3").build();
        ReflectionTestUtils.setField(user1, "id", 1L);
        ReflectionTestUtils.setField(user2, "id", 2L);
        ReflectionTestUtils.setField(user3, "id", 3L);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.ROUND_START_10M_BEFORE)
                .title("10분 후 토론이 시작돼요")
                .messageTemplate("곧 화상 토론이 열려요.")
                .linkTemplate("/users/me/meetings/{meetingId}")
                .build();

        given(notificationTypeRepository.findByCodeAndDeletedAtIsNull(NotificationTypeCode.ROUND_START_10M_BEFORE))
                .willReturn(Optional.of(type));
        given(userRepository.findAllById(userIds)).willReturn(List.of(user1, user2, user3));
        given(templateRenderer.render(anyString(), any())).willAnswer(inv -> inv.getArgument(0));

        // when
        notificationService.createAndSendBatch(
                userIds,
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                Map.of("meetingId", "123")
        );

        // then
        then(notificationRepository).should().saveAll(anyList());
        then(sseEmitterService).should().sendToUsers(eq(userIds), any());
        then(fcmService).should().sendToUsers(eq(userIds), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createAndSendBatch: 빈 사용자 목록이면 아무것도 하지 않는다")
    void createAndSendBatch_emptyUserIds_doesNothing() {
        // when
        notificationService.createAndSendBatch(
                List.of(),
                NotificationTypeCode.ROUND_START_10M_BEFORE,
                Map.of()
        );

        // then
        then(notificationRepository).should(never()).saveAll(anyList());
        then(fcmService).should(never()).sendToUsers(anyList(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getNotifications: 최근 3일 알림 목록을 반환한다")
    void getNotifications_success() {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.BOOK_REPORT_CHECKED)
                .title("독후감 검사가 완료됐어요")
                .messageTemplate("검사 결과를 확인해 주세요.")
                .build();

        Notification notification1 = Notification.builder()
                .user(user)
                .type(type)
                .title("독후감 검사가 완료됐어요")
                .message("검사 결과를 확인해 주세요.")
                .linkPath("/users/me/meetings/1")
                .build();
        ReflectionTestUtils.setField(notification1, "id", 1L);
        ReflectionTestUtils.setField(notification1, "isRead", false);
        ReflectionTestUtils.setField(notification1, "createdAt", LocalDateTime.now());

        Notification notification2 = Notification.builder()
                .user(user)
                .type(type)
                .title("독후감 검사가 완료됐어요")
                .message("검사 결과를 확인해 주세요.")
                .linkPath("/users/me/meetings/2")
                .build();
        ReflectionTestUtils.setField(notification2, "id", 2L);
        ReflectionTestUtils.setField(notification2, "isRead", true);
        ReflectionTestUtils.setField(notification2, "createdAt", LocalDateTime.now().minusHours(1));

        given(notificationRepository.findRecentByUserId(eq(userId), any(LocalDateTime.class)))
                .willReturn(List.of(notification1, notification2));

        // when
        NotificationListResponse response = notificationService.getNotifications(userId);

        // then
        assertThat(response.notifications()).hasSize(2);
        assertThat(response.hasUnread()).isTrue();
    }

    @Test
    @DisplayName("getNotifications: 읽지 않은 알림이 없으면 hasUnread가 false다")
    void getNotifications_allRead_hasUnreadFalse() {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.BOOK_REPORT_CHECKED)
                .title("Title")
                .messageTemplate("Message")
                .build();

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title("Title")
                .message("Message")
                .build();
        ReflectionTestUtils.setField(notification, "id", 1L);
        ReflectionTestUtils.setField(notification, "isRead", true);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());

        given(notificationRepository.findRecentByUserId(eq(userId), any(LocalDateTime.class)))
                .willReturn(List.of(notification));

        // when
        NotificationListResponse response = notificationService.getNotifications(userId);

        // then
        assertThat(response.hasUnread()).isFalse();
    }

    @Test
    @DisplayName("hasUnread: 읽지 않은 알림 존재 여부를 반환한다")
    void hasUnread_success() {
        // given
        Long userId = 1L;
        given(notificationRepository.existsUnreadByUserIdSince(eq(userId), any(LocalDateTime.class)))
                .willReturn(true);

        // when
        HasUnreadResponse response = notificationService.hasUnread(userId);

        // then
        assertThat(response.hasUnread()).isTrue();
    }

    @Test
    @DisplayName("markAsRead: 알림을 읽음 처리한다")
    void markAsRead_success() {
        // given
        Long userId = 1L;
        Long notificationId = 100L;

        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.BOOK_REPORT_CHECKED)
                .title("Title")
                .messageTemplate("Message")
                .build();

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title("Title")
                .message("Message")
                .build();
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(userId, notificationId);

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("markAsRead: 다른 사용자의 알림은 읽을 수 없다")
    void markAsRead_differentUser_throws() {
        // given
        Long requestUserId = 1L;
        Long notificationId = 100L;

        User owner = User.builder().nickname("owner").build();
        ReflectionTestUtils.setField(owner, "id", 99L);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.BOOK_REPORT_CHECKED)
                .title("Title")
                .messageTemplate("Message")
                .build();

        Notification notification = Notification.builder()
                .user(owner)
                .type(type)
                .title("Title")
                .message("Message")
                .build();
        ReflectionTestUtils.setField(notification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(requestUserId, notificationId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_FORBIDDEN);
    }

    @Test
    @DisplayName("markAsRead: 알림이 없으면 예외가 발생한다")
    void markAsRead_notFound_throws() {
        // given
        given(notificationRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("markAllAsRead: 모든 알림을 읽음 처리한다")
    void markAllAsRead_success() {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        NotificationType type = NotificationType.builder()
                .code(NotificationTypeCode.BOOK_REPORT_CHECKED)
                .title("Title")
                .messageTemplate("Message")
                .build();

        Notification notification1 = Notification.builder()
                .user(user).type(type).title("Title").message("Message").build();
        ReflectionTestUtils.setField(notification1, "id", 1L);
        ReflectionTestUtils.setField(notification1, "isRead", false);

        Notification notification2 = Notification.builder()
                .user(user).type(type).title("Title").message("Message").build();
        ReflectionTestUtils.setField(notification2, "id", 2L);
        ReflectionTestUtils.setField(notification2, "isRead", false);

        given(notificationRepository.findRecentByUserId(eq(userId), any(LocalDateTime.class)))
                .willReturn(List.of(notification1, notification2));

        // when
        notificationService.markAllAsRead(userId);

        // then
        assertThat(notification1.isRead()).isTrue();
        assertThat(notification2.isRead()).isTrue();
    }
}
