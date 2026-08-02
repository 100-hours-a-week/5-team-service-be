package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.UserPushToken;
import com.example.doktoribackend.notification.dto.FcmSendResult;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private static final int BATCH_SIZE = 500;

    private final FcmMessageSender fcmMessageSender;
    private final UserPushTokenRepository userPushTokenRepository;

    public void sendToUser(Long userId, String title, String body, String linkPath) {
        List<UserPushToken> tokens = userPushTokenRepository
                .findByUserIdsWithNotificationEnabled(List.of(userId));

        if (tokens.isEmpty()) {
            log.debug("No FCM token found for userId: {}", userId);
            return;
        }

        try {
            fcmMessageSender.send(richMessage(tokens.getFirst().getToken(), title, body, linkPath));
        } catch (Exception e) {
            log.error("Failed to send FCM message - userId={}, error={}: {}",
                    userId, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 배치 전송 후 응답을 성공 / 토큰 폐기 / 영구 실패 / 일시 실패로 분류해 반환한다.
     *
     * <p>예외를 던지지 않는다. 호출자(컨슈머)가 결과를 보고 재시도 범위를 정하며,
     * 실패한 사용자만 재전송하므로 성공한 사용자에게 중복 푸시가 가지 않는다.
     */
    public FcmSendResult sendToUsers(List<Long> userIds, String title, String body, String linkPath) {
        List<UserPushToken> tokens = userPushTokenRepository
                .findByUserIdsWithNotificationEnabled(userIds);

        if (tokens.isEmpty()) {
            return FcmSendResult.empty();
        }

        List<Message> messages = tokens.stream()
                .map(pushToken -> simpleMessage(pushToken.getToken(), title, body, linkPath))
                .toList();

        int successCount = 0;
        List<String> discardedTokens = new ArrayList<>();
        List<Long> retryableUserIds = new ArrayList<>();
        boolean totalFailure = false;

        for (int offset = 0; offset < messages.size(); offset += BATCH_SIZE) {
            int end = Math.min(offset + BATCH_SIZE, messages.size());
            List<UserPushToken> chunkTokens = tokens.subList(offset, end);

            try {
                BatchResponse response = fcmMessageSender.sendEach(messages.subList(offset, end));
                successCount += classifyChunk(chunkTokens, response, discardedTokens, retryableUserIds);
            } catch (Exception e) {
                // 배치 호출 자체의 실패(FCM 다운, 타임아웃, 서킷 Open). 이 청크는 전원 재시도 대상.
                log.error("FCM 배치 전송 실패 - size={}, error={}: {}",
                        chunkTokens.size(), e.getClass().getSimpleName(), e.getMessage());
                totalFailure = true;
                chunkTokens.forEach(pushToken -> retryableUserIds.add(pushToken.getUserId()));
            }
        }

        if (!discardedTokens.isEmpty()) {
            cleanupInvalidTokens(discardedTokens);
        }

        log.info("FCM 전송 결과 - success={}, discarded={}, retryable={}, totalFailure={}",
                successCount, discardedTokens.size(), retryableUserIds.size(), totalFailure);

        return totalFailure
                ? new FcmSendResult(successCount, List.copyOf(discardedTokens), List.copyOf(retryableUserIds), true)
                : FcmSendResult.of(successCount, discardedTokens, retryableUserIds);
    }

    private int classifyChunk(List<UserPushToken> chunkTokens,
                              BatchResponse response,
                              List<String> discardedTokens,
                              List<Long> retryableUserIds) {
        List<SendResponse> responses = response.getResponses();
        int successCount = 0;

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            UserPushToken pushToken = chunkTokens.get(i);

            if (sendResponse.isSuccessful()) {
                successCount++;
                continue;
            }

            MessagingErrorCode errorCode = sendResponse.getException() == null
                    ? null
                    : sendResponse.getException().getMessagingErrorCode();

            switch (FcmErrorClassifier.classify(errorCode)) {
                case DISCARD_TOKEN -> discardedTokens.add(pushToken.getToken());
                case PERMANENT -> log.error("FCM 설정 오류로 전송 불가 - userId={}, errorCode={}",
                        pushToken.getUserId(), errorCode);
                case TRANSIENT -> retryableUserIds.add(pushToken.getUserId());
            }
        }

        return successCount;
    }

    private void cleanupInvalidTokens(List<String> invalidTokens) {
        for (String token : invalidTokens) {
            userPushTokenRepository.findByToken(token)
                    .ifPresent(userPushTokenRepository::delete);
        }
        log.info("Cleaned up {} invalid FCM tokens", invalidTokens.size());
    }

    private Message simpleMessage(String token, String title, String body, String linkPath) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("linkPath", linkPath != null ? linkPath : "")
                .build();
    }

    private Message richMessage(String token, String title, String body, String linkPath) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("linkPath", linkPath != null ? linkPath : "")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .setSound("default")
                                .build())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .build();
    }
}
