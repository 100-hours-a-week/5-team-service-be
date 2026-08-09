package com.example.doktoribackend.notification.service;

import com.google.firebase.messaging.MessagingErrorCode;

final class FcmErrorClassifier {

    private FcmErrorClassifier() {
    }

    static FcmFailureType classify(MessagingErrorCode errorCode) {
        if (errorCode == null) {
            // 메시징 계층 밖의 오류. 유실보다 재시도가 안전하다.
            return FcmFailureType.TRANSIENT;
        }

        return switch (errorCode) {
            case UNREGISTERED, INVALID_ARGUMENT -> FcmFailureType.DISCARD_TOKEN;
            case SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR -> FcmFailureType.PERMANENT;
            case UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED -> FcmFailureType.TRANSIENT;
        };
    }
}
