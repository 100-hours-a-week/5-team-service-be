package com.example.doktoribackend.notification.dto;

import java.util.List;

/**
 * FCM 배치 전송 결과. 컨슈머가 재시도 여부와 범위를 판단하는 재료다.
 *
 * @param successCount     전송에 성공한 메시지 수
 * @param discardedTokens  무효로 판정되어 삭제한 토큰
 * @param retryableUserIds 일시 실패로 재전송이 필요한 사용자. 성공한 사용자는 포함되지 않으므로
 *                         재시도 시 중복 발송이 생기지 않는다.
 * @param totalFailure     배치 호출 자체가 실패했는지(FCM 다운 또는 서킷 Open)
 */
public record FcmSendResult(
        int successCount,
        List<String> discardedTokens,
        List<Long> retryableUserIds,
        boolean totalFailure
) {

    public static FcmSendResult of(int successCount, List<String> discardedTokens, List<Long> retryableUserIds) {
        return new FcmSendResult(successCount, List.copyOf(discardedTokens), List.copyOf(retryableUserIds), false);
    }

    public static FcmSendResult totalFailure(List<Long> userIds) {
        return new FcmSendResult(0, List.of(), List.copyOf(userIds), true);
    }

    public static FcmSendResult empty() {
        return new FcmSendResult(0, List.of(), List.of(), false);
    }
}
