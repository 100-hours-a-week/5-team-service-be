package com.example.doktoribackend.review.domain;

/**
 * 사용자에게 보여줄 후기 작성 상태.
 * <ul>
 *   <li>NOT_YET_WRITABLE – 회차 미종료 또는 독후감 미승인</li>
 *   <li>NOT_SUBMITTED – 작성 가능하지만 아직 미제출</li>
 *   <li>SUBMITTED – 이미 제출 완료</li>
 *   <li>DEADLINE_PASSED – 회차 종료 후 24시간 경과</li>
 * </ul>
 */
public enum UserReviewStatus {
    NOT_YET_WRITABLE,
    NOT_SUBMITTED,
    SUBMITTED,
    DEADLINE_PASSED
}
