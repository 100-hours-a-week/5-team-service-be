package com.example.doktoribackend.notification.service;

import com.example.doktoribackend.notification.domain.UserPushToken;
import com.example.doktoribackend.notification.repository.UserPushTokenRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserPushTokenRepository userPushTokenRepository;
    private static final int BATCH_SIZE = 500;

    @Async("notificationExecutor")
    public void sendToUser(Long userId, String title, String body, String linkPath) {
        List<UserPushToken> tokens = userPushTokenRepository
                .findByUserIdsWithNotificationEnabled(List.of(userId));

        if (tokens.isEmpty()) {
            log.debug("No FCM token found for userId: {}", userId);
            return;
        }

        sendToToken(tokens.getFirst().getToken(), title, body, linkPath);
    }

    @Async("notificationExecutor")
    public void sendToUsers(List<Long> userIds, String title, String body, String linkPath) {
        List<UserPushToken> tokens = userPushTokenRepository
                .findByUserIdsWithNotificationEnabled(userIds);

        if (tokens.isEmpty()) {
            return;
        }

        List<String> failedTokens = sendBatch(tokens, title, body, linkPath);
        if (!failedTokens.isEmpty()) {
            cleanupInvalidTokens(failedTokens);
        }
    }

    private void sendToToken(String token, String title, String body, String linkPath) {
        Message message = Message.builder()
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

        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token: {}", token, e);
        }
    }

    private List<String> sendBatch(List<UserPushToken> pushTokens, String title, String body, String linkPath) {
        List<Message> messages = pushTokens.stream()
                .map(pt -> Message.builder()
                        .setToken(pt.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("linkPath", linkPath != null ? linkPath : "")
                        .build())
                .toList();

        List<String> failedTokens = new ArrayList<>();

        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, messages.size());
            List<Message> batch = messages.subList(i, end);

            try {
                BatchResponse response = firebaseMessaging.sendEach(batch);
                log.info("FCM batch send - success: {}, failure: {}",
                        response.getSuccessCount(), response.getFailureCount());

                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    SendResponse sendResponse = responses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                        if (errorCode == MessagingErrorCode.UNREGISTERED
                                || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                            failedTokens.add(pushTokens.get(i + j).getToken());
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("Batch FCM send failed", e);
            }
        }

        return failedTokens;
    }

    private void cleanupInvalidTokens(List<String> invalidTokens) {
        for (String token : invalidTokens) {
            userPushTokenRepository.findByToken(token)
                    .ifPresent(userPushTokenRepository::delete);
        }
        log.info("Cleaned up {} invalid FCM tokens", invalidTokens.size());
    }
}
