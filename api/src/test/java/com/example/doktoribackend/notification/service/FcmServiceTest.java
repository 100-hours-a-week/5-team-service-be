package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.Platform;
import com.example.doktoribackend.notification.domain.PushProvider;
import com.example.doktoribackend.notification.domain.UserPushToken;
import com.example.doktoribackend.notification.dto.FcmSendResult;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock
    private FcmMessageSender fcmMessageSender;

    @Mock
    private UserPushTokenRepository userPushTokenRepository;

    private FcmService fcmService;

    @BeforeEach
    void setUp() {
        fcmService = new FcmService(fcmMessageSender, userPushTokenRepository);
    }

    @Nested
    @DisplayName("단건 전송")
    class SingleSend {

        @Test
        @DisplayName("sendToUser: FCM 토큰이 있으면 푸시를 전송한다")
        void sendToUser_withToken_sendsPush() throws Exception {
            given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(1L)))
                    .willReturn(List.of(pushToken(1L, "fcm-token-123")));
            given(fcmMessageSender.send(any(Message.class))).willReturn("message-id");

            fcmService.sendToUser(1L, "제목", "내용", "/link");

            then(fcmMessageSender).should().send(any(Message.class));
        }

        @Test
        @DisplayName("sendToUser: FCM 토큰이 없으면 전송하지 않는다")
        void sendToUser_noToken_doesNotSend() throws Exception {
            given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(1L)))
                    .willReturn(List.of());

            fcmService.sendToUser(1L, "제목", "내용", "/link");

            then(fcmMessageSender).should(never()).send(any(Message.class));
        }

        @Test
        @DisplayName("sendToUser: FCM 전송 실패해도 예외를 던지지 않는다")
        void sendToUser_fcmFails_doesNotThrow() throws Exception {
            given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(List.of(1L)))
                    .willReturn(List.of(pushToken(1L, "fcm-token-123")));
            given(fcmMessageSender.send(any(Message.class)))
                    .willThrow(mock(FirebaseMessagingException.class));

            fcmService.sendToUser(1L, "제목", "내용", "/link");

            then(fcmMessageSender).should().send(any(Message.class));
        }
    }

    @Nested
    @DisplayName("배치 전송 결과 분류")
    class BatchSend {

        @Test
        @DisplayName("sendToUsers: 전원 성공하면 재시도 대상도 폐기 토큰도 없다")
        void sendToUsers_allSucceed_noRetryNoDiscard() throws Exception {
            givenTokens(pushToken(1L, "token-1"), pushToken(2L, "token-2"));
            givenBatchResponse(success(), success());

            FcmSendResult result = fcmService.sendToUsers(List.of(1L, 2L), "제목", "본문", "/link");

            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.retryableUserIds()).isEmpty();
            assertThat(result.discardedTokens()).isEmpty();
            assertThat(result.totalFailure()).isFalse();
            verify(userPushTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("sendToUsers: UNREGISTERED 토큰은 삭제하고 재시도하지 않는다")
        void sendToUsers_unregistered_discardsTokenWithoutRetry() throws Exception {
            UserPushToken dead = pushToken(2L, "token-2");
            givenTokens(pushToken(1L, "token-1"), dead);
            givenBatchResponse(success(), failure(MessagingErrorCode.UNREGISTERED));
            given(userPushTokenRepository.findByToken("token-2")).willReturn(Optional.of(dead));

            FcmSendResult result = fcmService.sendToUsers(List.of(1L, 2L), "제목", "본문", "/link");

            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.discardedTokens()).containsExactly("token-2");
            assertThat(result.retryableUserIds()).isEmpty();
            verify(userPushTokenRepository).delete(dead);
        }

        @Test
        @DisplayName("sendToUsers: UNAVAILABLE 은 재시도 대상이며 토큰을 삭제하지 않는다")
        void sendToUsers_unavailable_marksRetryableKeepsToken() throws Exception {
            givenTokens(pushToken(1L, "token-1"), pushToken(2L, "token-2"));
            givenBatchResponse(success(), failure(MessagingErrorCode.UNAVAILABLE));

            FcmSendResult result = fcmService.sendToUsers(List.of(1L, 2L), "제목", "본문", "/link");

            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.retryableUserIds()).containsExactly(2L);
            assertThat(result.discardedTokens()).isEmpty();
            verify(userPushTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("sendToUsers: SENDER_ID_MISMATCH 는 재시도해도 소용없으므로 재시도 대상이 아니다")
        void sendToUsers_senderIdMismatch_notRetryable() throws Exception {
            givenTokens(pushToken(1L, "token-1"));
            givenBatchResponse(failure(MessagingErrorCode.SENDER_ID_MISMATCH));

            FcmSendResult result = fcmService.sendToUsers(List.of(1L), "제목", "본문", "/link");

            assertThat(result.retryableUserIds()).isEmpty();
            assertThat(result.discardedTokens()).isEmpty();
            assertThat(result.totalFailure()).isFalse();
            verify(userPushTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("sendToUsers: 배치 호출 자체가 실패하면 전원이 재시도 대상이 된다")
        void sendToUsers_batchCallFails_allRetryable() throws Exception {
            givenTokens(pushToken(1L, "token-1"), pushToken(2L, "token-2"));
            given(fcmMessageSender.sendEach(anyList())).willThrow(mock(FirebaseMessagingException.class));

            FcmSendResult result = fcmService.sendToUsers(List.of(1L, 2L), "제목", "본문", "/link");

            assertThat(result.totalFailure()).isTrue();
            assertThat(result.retryableUserIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(result.successCount()).isZero();
        }

        @Test
        @DisplayName("sendToUsers: 서킷이 열려 차단되면 유실하지 않고 전원을 재시도 대상으로 둔다")
        void sendToUsers_circuitOpen_allRetryable() throws Exception {
            givenTokens(pushToken(1L, "token-1"));
            given(fcmMessageSender.sendEach(anyList())).willThrow(circuitOpenException());

            FcmSendResult result = fcmService.sendToUsers(List.of(1L), "제목", "본문", "/link");

            assertThat(result.totalFailure()).isTrue();
            assertThat(result.retryableUserIds()).containsExactly(1L);
        }

        @Test
        @DisplayName("sendToUsers: 등록된 토큰이 없으면 FCM 을 호출하지 않는다")
        void sendToUsers_noTokens_doesNotCallFcm() throws Exception {
            given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(anyList()))
                    .willReturn(List.of());

            FcmSendResult result = fcmService.sendToUsers(List.of(1L), "제목", "본문", "/link");

            assertThat(result.successCount()).isZero();
            assertThat(result.retryableUserIds()).isEmpty();
            assertThat(result.totalFailure()).isFalse();
            verify(fcmMessageSender, never()).sendEach(anyList());
        }
    }

    private void givenTokens(UserPushToken... tokens) {
        given(userPushTokenRepository.findByUserIdsWithNotificationEnabled(anyList()))
                .willReturn(List.of(tokens));
    }

    private void givenBatchResponse(SendResponse... responses) throws Exception {
        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of(responses));
        given(fcmMessageSender.sendEach(anyList())).willReturn(batchResponse);
    }

    private SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private SendResponse failure(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(code);
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }

    private UserPushToken pushToken(Long userId, String token) {
        UserPushToken pushToken = UserPushToken.builder()
                .platform(Platform.ANDROID)
                .provider(PushProvider.FCM)
                .token(token)
                .build();
        // userId 는 @MapsId 로 영속 시점에 채워지므로 단위 테스트에서는 직접 주입한다.
        ReflectionTestUtils.setField(pushToken, "userId", userId);
        return pushToken;
    }

    private CallNotPermittedException circuitOpenException() {
        CircuitBreaker breaker = CircuitBreaker.of("fcm", CircuitBreakerConfig.ofDefaults());
        breaker.transitionToOpenState();
        return CallNotPermittedException.createCallNotPermittedException(breaker);
    }
}
