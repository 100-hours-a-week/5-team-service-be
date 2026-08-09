package com.example.doktoribackend.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FCM 호출에 서킷 브레이커를 거는 얇은 래퍼.
 *
 * <p>{@link FcmService} 안에서 자기호출하면 Spring 프록시를 우회해 애노테이션이 무시되므로
 * 별도 빈으로 분리했다.
 *
 * <p>예외를 삼키지 않는다. 배치 호출 자체의 실패만 서킷에 집계되어야 하며, 개별 메시지의
 * UNREGISTERED 같은 결과는 FCM 이 정상 동작한 응답이므로 여기서 예외가 되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FcmMessageSender {

    private final FirebaseMessaging firebaseMessaging;

    @CircuitBreaker(name = "fcm")
    public BatchResponse sendEach(List<Message> messages) throws FirebaseMessagingException {
        return firebaseMessaging.sendEach(messages);
    }

    @CircuitBreaker(name = "fcm")
    public String send(Message message) throws FirebaseMessagingException {
        return firebaseMessaging.send(message);
    }
}
