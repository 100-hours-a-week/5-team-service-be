-- 회차 베스트 모임원 산정 완료 추적 컬럼 추가
ALTER TABLE meeting_rounds ADD COLUMN best_member_determined BOOLEAN NOT NULL DEFAULT FALSE;

-- 후기 작성 요청 알림 타입
INSERT INTO notification_types (code, title, message_template, link_template, created_at, updated_at)
VALUES ('ROUND_COMPLETED_REVIEW_REQUEST', '후기 작성 요청',
        '{meetingTitle} {roundNo}회차가 종료되었습니다. 후기를 작성해주세요!',
        '/my-meeting/{roundId}/review',
        NOW(6), NOW(6));

-- 베스트 모임원 선정 알림 타입
INSERT INTO notification_types (code, title, message_template, link_template, created_at, updated_at)
VALUES ('BEST_MEMBER_SELECTED', '베스트 모임원 선정',
        '축하합니다! {meetingTitle} {roundNo}회차에서 베스트 모임원으로 선정되었습니다!',
        '/my-meeting/{meetingId}',
        NOW(6), NOW(6));
