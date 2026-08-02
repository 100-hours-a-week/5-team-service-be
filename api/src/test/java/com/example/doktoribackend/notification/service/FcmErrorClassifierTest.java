package com.example.doktoribackend.notification.service;

import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class FcmErrorClassifierTest {

    @Test
    @DisplayName("classify: 토큰이 죽은 경우는 폐기 대상이다")
    void classify_deadToken_isDiscardToken() {
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.UNREGISTERED))
                .isEqualTo(FcmFailureType.DISCARD_TOKEN);
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.INVALID_ARGUMENT))
                .isEqualTo(FcmFailureType.DISCARD_TOKEN);
    }

    @Test
    @DisplayName("classify: 발신자·인증서 설정 문제는 영구 실패지만 토큰은 유지한다")
    void classify_configurationError_isPermanent() {
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.SENDER_ID_MISMATCH))
                .isEqualTo(FcmFailureType.PERMANENT);
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR))
                .isEqualTo(FcmFailureType.PERMANENT);
    }

    @Test
    @DisplayName("classify: 서버 과부하·한도 초과는 재시도 대상이다")
    void classify_serverSideError_isTransient() {
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.UNAVAILABLE))
                .isEqualTo(FcmFailureType.TRANSIENT);
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.INTERNAL))
                .isEqualTo(FcmFailureType.TRANSIENT);
        assertThat(FcmErrorClassifier.classify(MessagingErrorCode.QUOTA_EXCEEDED))
                .isEqualTo(FcmFailureType.TRANSIENT);
    }

    @Test
    @DisplayName("classify: 에러코드가 없으면 유실보다 재시도가 안전하므로 일시 실패로 본다")
    void classify_nullCode_isTransient() {
        assertThat(FcmErrorClassifier.classify(null)).isEqualTo(FcmFailureType.TRANSIENT);
    }

    @ParameterizedTest
    @EnumSource(MessagingErrorCode.class)
    @DisplayName("classify: 모든 MessagingErrorCode 가 분류를 가진다 (SDK 업그레이드 시 누락 방지)")
    void classify_everyErrorCode_hasClassification(MessagingErrorCode code) {
        assertThat(FcmErrorClassifier.classify(code)).isNotNull();
    }
}
