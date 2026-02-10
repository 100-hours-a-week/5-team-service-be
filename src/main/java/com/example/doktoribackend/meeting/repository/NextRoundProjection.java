package com.example.doktoribackend.meeting.repository;

import java.time.LocalDateTime;

/**
 * 모임의 다음 회차 조회 결과를 담는 Projection 인터페이스
 */
public interface NextRoundProjection {
    Long getMeetingId();
    LocalDateTime getNextRoundDate();
}