-- notification_types: 템플릿 경로 변경
UPDATE notification_types
SET link_template = '/my-meeting/{meetingId}'
WHERE link_template = '/users/me/meetings/{meetingId}';

-- notifications: 이미 저장된 link_path 경로 일괄 변경 (/users/me/meetings/123 → /my-meeting/123)
UPDATE notifications
SET link_path = CONCAT('/my-meeting/', SUBSTRING_INDEX(link_path, '/', -1))
WHERE link_path LIKE '/users/me/meetings/%';
