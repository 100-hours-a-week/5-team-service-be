package com.example.doktoribackend.notification.service;

/**
 * FCM 개별 메시지 실패의 성격.
 *
 * <p>재시도 여부를 이 분류로 결정한다. 죽은 토큰을 5초·15초·45초 백오프로 재시도하는 것은
 * 결과가 같은 낭비이고, 반대로 서버 과부하를 영구 실패로 취급하면 알림이 유실된다.
 */
public enum FcmFailureType {

    /** 토큰이 더 이상 유효하지 않다. 토큰을 삭제하고 재시도하지 않는다. */
    DISCARD_TOKEN,

    /** 발신자·인증서 설정 문제. 재시도해도 동일하게 실패하므로 토큰은 두고 재시도만 포기한다. */
    PERMANENT,

    /** FCM 서버 측 일시 장애. 재시도 대상. */
    TRANSIENT
}
