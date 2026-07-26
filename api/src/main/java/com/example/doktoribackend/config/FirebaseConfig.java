package com.example.doktoribackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path}")
    private Resource credentialsResource;

    /**
     * FCM 은 SDK 호출이라 RestClient 타임아웃이 적용되지 않는다. SDK 자체 설정으로 지정한다.
     * 미설정 시 SDK 기본값(사실상 무제한)이라 FCM 지연이 알림 스레드를 그대로 묶는다.
     */
    @Value("${firebase.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${firebase.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                credentialsResource.getInputStream()))
                        .setConnectTimeout(connectTimeoutMs)
                        .setReadTimeout(readTimeoutMs)
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Firebase initialization failed", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
