-- meeting_bookmarks 테이블에 대리키 추가
-- 기존 복합 PK를 대리키 PK + UNIQUE 제약으로 변경

-- 1. 기존 테이블 삭제 (데이터 없는 상태이므로 재생성)
DROP TABLE IF EXISTS meeting_bookmarks;

-- 2. 대리키 포함하여 재생성
CREATE TABLE meeting_bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_meeting_bookmark UNIQUE (user_id, meeting_id),
    CONSTRAINT fk_meeting_bookmark_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_meeting_bookmark_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    INDEX idx_meeting_bookmark_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;