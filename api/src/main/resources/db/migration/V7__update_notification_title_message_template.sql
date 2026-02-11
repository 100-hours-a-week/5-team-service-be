-- notification_types: title에 이모지 추가, message에 모임 이름 플레이스홀더 추가
UPDATE notification_types
SET title = '10분 후 토론이 시작돼요',
    message_template = '[{meetingTitle}] 곧 화상 토론이 열려요. 지금 접속 환경을 한 번 확인해 주세요.'
WHERE code = 'ROUND_START_10M_BEFORE';

UPDATE notification_types
SET title = '독후감 검사가 완료됐어요',
    message_template = '[{meetingTitle}] 검사 결과를 확인해 주세요.'
WHERE code = 'BOOK_REPORT_CHECKED';

UPDATE notification_types
SET title = '독후감 마감까지 하루 남았어요',
    message_template = '[{meetingTitle}] 독후감을 제출해야 토론 모임에 참여할 수 있어요.'
WHERE code = 'BOOK_REPORT_DEADLINE_24H_BEFORE';

UPDATE notification_types
SET title = '독후감 마감까지 30분 남았어요',
    message_template = '[{meetingTitle}] 지금 제출하면 토론에 참여할 수 있어요.'
WHERE code = 'BOOK_REPORT_DEADLINE_30M_BEFORE';
