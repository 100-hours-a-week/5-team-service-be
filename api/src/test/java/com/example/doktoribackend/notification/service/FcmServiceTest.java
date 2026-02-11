package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.Platform;
import com.example.doktoribackend.notification.domain.PushProvider;
import com.example.doktoribackend.notification.domain.UserPushToken;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.example.doktoribackend.user.domain.User;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock
    FirebaseMessaging firebaseMessaging;

    @Mock
    UserPushTokenRepository userPushTokenRepository;

    @InjectMocks
    FcmService fcmService;

    @Test
    @DisplayName("sendToUser: FCM 토큰이 있으면 푸시를 전송한다")
    void sendToUser_withToken_sendsPush() throws FirebaseMessagingException {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        UserPushToken pushToken = UserPushToken.builder()
                .user(user)
                .platform(Platform.ANDROID)
                .provider(PushProvider.FCM)
                .token("fcm-token-123")
                .build();

        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(userId)))
                .willReturn(List.of(pushToken));
        given(firebaseMessaging.send(any(Message.class))).willReturn("message-id");

        // when
        fcmService.sendToUser(userId, "제목", "내용", "/link");

        // then
        then(firebaseMessaging).should().send(any(Message.class));
    }

    @Test
    @DisplayName("sendToUser: FCM 토큰이 없으면 전송하지 않는다")
    void sendToUser_noToken_doesNotSend() throws FirebaseMessagingException {
        // given
        Long userId = 1L;
        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(userId)))
                .willReturn(List.of());

        // when
        fcmService.sendToUser(userId, "제목", "내용", "/link");

        // then
        then(firebaseMessaging).should(never()).send(any(Message.class));
    }

    @Test
    @DisplayName("sendToUser: FCM 전송 실패해도 예외를 던지지 않는다")
    void sendToUser_fcmFails_doesNotThrow() throws FirebaseMessagingException {
        // given
        Long userId = 1L;
        User user = User.builder().nickname("testUser").build();
        ReflectionTestUtils.setField(user, "id", userId);

        UserPushToken pushToken = UserPushToken.builder()
                .user(user)
                .platform(Platform.ANDROID)
                .provider(PushProvider.FCM)
                .token("fcm-token-123")
                .build();

        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(userId)))
                .willReturn(List.of(pushToken));
        given(firebaseMessaging.send(any(Message.class)))
                .willThrow(mock(FirebaseMessagingException.class));

        // when - 예외가 발생하지 않아야 함
        fcmService.sendToUser(userId, "제목", "내용", "/link");

        // then
        then(firebaseMessaging).should().send(any(Message.class));
    }

    @Test
    @DisplayName("sendToUsers: 여러 사용자에게 일괄 전송한다")
    void sendToUsers_batchSend_success() throws FirebaseMessagingException {
        // given
        List<Long> userIds = List.of(1L, 2L);

        User user1 = User.builder().nickname("user1").build();
        User user2 = User.builder().nickname("user2").build();
        ReflectionTestUtils.setField(user1, "id", 1L);
        ReflectionTestUtils.setField(user2, "id", 2L);

        UserPushToken token1 = UserPushToken.builder()
                .user(user1).platform(Platform.ANDROID).provider(PushProvider.FCM).token("token1").build();
        UserPushToken token2 = UserPushToken.builder()
                .user(user2).platform(Platform.IOS).provider(PushProvider.FCM).token("token2").build();

        BatchResponse batchResponse = mock(BatchResponse.class);
        doReturn(2).when(batchResponse).getSuccessCount();
        doReturn(0).when(batchResponse).getFailureCount();
        doReturn(List.of(
                mockSuccessResponse(),
                mockSuccessResponse()
        )).when(batchResponse).getResponses();

        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(userIds))
                .willReturn(List.of(token1, token2));
        doReturn(batchResponse).when(firebaseMessaging).sendEach(anyList());

        // when
        fcmService.sendToUsers(userIds, "제목", "내용", "/link");

        // then
        then(firebaseMessaging).should().sendEach(anyList());
    }

    @Test
    @DisplayName("sendToUsers: 토큰이 없으면 전송하지 않는다")
    void sendToUsers_noTokens_doesNotSend() throws FirebaseMessagingException {
        // given
        List<Long> userIds = List.of(1L, 2L);
        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(userIds))
                .willReturn(List.of());

        // when
        fcmService.sendToUsers(userIds, "제목", "내용", "/link");

        // then
        then(firebaseMessaging).should(never()).sendEach(anyList());
    }

    @Test
    @DisplayName("sendToUsers: 유효하지 않은 토큰은 삭제한다")
    void sendToUsers_invalidToken_cleansUp() throws FirebaseMessagingException {
        // given
        List<Long> userIds = List.of(1L);

        User user = User.builder().nickname("user1").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserPushToken token = UserPushToken.builder()
                .user(user)
                .platform(Platform.ANDROID)
                .provider(PushProvider.FCM)
                .token("invalid-token")
                .build();
        BatchResponse batchResponse = mock(BatchResponse.class);
        doReturn(0).when(batchResponse).getSuccessCount();
        doReturn(1).when(batchResponse).getFailureCount();
        doReturn(List.of(
                mockFailureResponse(MessagingErrorCode.UNREGISTERED)
        )).when(batchResponse).getResponses();

        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(userIds))
                .willReturn(List.of(token));
        doReturn(batchResponse).when(firebaseMessaging).sendEach(anyList());

        given(userPushTokenRepository.findByToken("invalid-token"))
                .willReturn(Optional.of(token));
        // when
        fcmService.sendToUsers(userIds, "제목", "내용", "/link");

        // then
        then(userPushTokenRepository).should().delete(token);
    }

    private SendResponse mockSuccessResponse() {
        SendResponse response = mock(SendResponse.class);
        doReturn(true).when(response).isSuccessful();
        return response;
    }

    private SendResponse mockFailureResponse(MessagingErrorCode errorCode) {
        SendResponse response = mock(SendResponse.class);
        doReturn(false).when(response).isSuccessful();

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(errorCode);
        given(response.getException()).willReturn(exception);

        return response;
    }
}
