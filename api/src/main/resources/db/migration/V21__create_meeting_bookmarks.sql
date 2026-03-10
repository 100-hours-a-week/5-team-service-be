-- meeting_bookmarks (관심 모임 북마크)
CREATE TABLE meeting_bookmarks (
    user_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, meeting_id),
    CONSTRAINT fk_meeting_bookmark_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_meeting_bookmark_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;