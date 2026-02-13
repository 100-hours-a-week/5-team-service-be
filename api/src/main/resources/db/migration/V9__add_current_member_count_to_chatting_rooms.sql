ALTER TABLE chatting_rooms
    ADD COLUMN current_member_count TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '현재 참여 인원수' AFTER capacity;
