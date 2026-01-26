package com.example.doktoribackend.notification.controller;

import com.example.doktoribackend.notification.domain.NotificationTypeCode;
import com.example.doktoribackend.notification.domain.Platform;
import com.example.doktoribackend.notification.dto.HasUnreadResponse;
import com.example.doktoribackend.notification.dto.NotificationListResponse;
import com.example.doktoribackend.notification.dto.NotificationResponse;
import com.example.doktoribackend.notification.dto.PushTokenRequest;
import com.example.doktoribackend.notification.service.NotificationService;
import com.example.doktoribackend.notification.service.PushTokenService;
import com.example.doktoribackend.notification.service.SseEmitterService;
import com.example.doktoribackend.security.CustomUserDetails;
import com.example.doktoribackend.security.CustomUserDetailsService;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.doktoribackend.user.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    PushTokenService pushTokenService;

    @MockitoBean
    SseEmitterService sseEmitterService;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /notifications: 알림 목록을 조회한다")
    void getNotifications_success() throws Exception {
        // given
        NotificationResponse notification = new NotificationResponse(
                1L,
                NotificationTypeCode.BOOK_REPORT_CHECKED,
                "독후감 검사가 완료됐어요",
                "검사 결과를 확인해 주세요.",
                "/users/me/meetings/123",
                false,
                LocalDateTime.now()
        );
        NotificationListResponse response = new NotificationListResponse(
                List.of(notification),
                true
        );

        given(notificationService.getNotifications(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/notifications")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifications").isArray())
                .andExpect(jsonPath("$.data.notifications[0].id").value(1))
                .andExpect(jsonPath("$.data.notifications[0].title").value("독후감 검사가 완료됐어요"))
                .andExpect(jsonPath("$.data.hasUnread").value(true));
    }

    @Test
    @DisplayName("GET /notifications/has-unread: 읽지 않은 알림 존재 여부를 반환한다")
    void hasUnread_success() throws Exception {
        // given
        given(notificationService.hasUnread(1L)).willReturn(new HasUnreadResponse(true));

        // when & then
        mockMvc.perform(get("/notifications/has-unread")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasUnread").value(true));
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read: 알림을 읽음 처리한다")
    void markAsRead_success() throws Exception {
        // when & then
        mockMvc.perform(put("/notifications/100/read")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        then(notificationService).should().markAsRead(1L, 100L);
    }

    @Test
    @DisplayName("PUT /notifications/read-all: 모든 알림을 읽음 처리한다")
    void markAllAsRead_success() throws Exception {
        // when & then
        mockMvc.perform(put("/notifications/read-all")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        then(notificationService).should().markAllAsRead(1L);
    }

    @Test
    @DisplayName("POST /notifications/push-token: FCM 토큰을 등록한다")
    void registerPushToken_success() throws Exception {
        // given
        PushTokenRequest request = new PushTokenRequest("fcm-token-123", Platform.ANDROID);

        // when & then
        mockMvc.perform(post("/notifications/push-token")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(pushTokenService).should().registerToken(1L, "fcm-token-123", Platform.ANDROID);
    }

    @Test
    @DisplayName("POST /notifications/push-token: 토큰이 없으면 400 에러")
    void registerPushToken_noToken_badRequest() throws Exception {
        // given
        String invalidRequest = """
                {
                    "token": "",
                    "platform": "ANDROID"
                }
                """;

        // when & then
        mockMvc.perform(post("/notifications/push-token")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /notifications/push-token: 플랫폼이 없으면 400 에러")
    void registerPushToken_noPlatform_badRequest() throws Exception {
        // given
        String invalidRequest = """
                {
                    "token": "fcm-token-123"
                }
                """;

        // when & then
        mockMvc.perform(post("/notifications/push-token")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /notifications/subscribe: SSE 연결을 생성한다")
    void subscribe_success() throws Exception {
        // given
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        given(sseEmitterService.subscribe(1L)).willReturn(emitter);

        // when & then
        mockMvc.perform(get("/notifications/subscribe")
                        .with(SecurityMockMvcRequestPostProcessors.user(createUserDetails(1L)))
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        then(sseEmitterService).should().subscribe(1L);
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 401 에러")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private CustomUserDetails createUserDetails(Long userId) {
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);
        return CustomUserDetails.from(user);
    }
}
