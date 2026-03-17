-- 독후감 찌르기 알림 타입
INSERT INTO notification_types (code, title, message_template, link_template, created_at, updated_at)
VALUES ('BOOK_REPORT_POKE', '독후감 작성 알림',
        '{meetingTitle} {roundNo}회차 독후감을 아직 제출하지 않았어요. 마감 전에 작성해주세요!',
        '/my-meeting/{meetingId}',
        NOW(6), NOW(6));
