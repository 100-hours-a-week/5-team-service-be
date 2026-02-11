-- book_reports
CREATE TABLE book_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT  NOT NULL,
    meeting_round_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason VARCHAR(200) NULL,
    ai_validated_at DATETIME NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_book_reports_user (user_id),
    INDEX idx_book_reports_meeting_round (meeting_round_id),
    CONSTRAINT fk_book_reports_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_book_reports_meeting_round FOREIGN KEY (meeting_round_id) REFERENCES meeting_rounds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
