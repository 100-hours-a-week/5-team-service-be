-- meeting_round_discussion_topics (회차 토론 주제)
CREATE TABLE meeting_round_discussion_topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_round_id BIGINT NOT NULL,
    topic_no TINYINT UNSIGNED NOT NULL,
    topic VARCHAR(120) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'AI',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_discussion_topic_meeting_round FOREIGN KEY (meeting_round_id) REFERENCES meeting_rounds(id),
    CONSTRAINT uk_meeting_round_topic_no UNIQUE (meeting_round_id, topic_no),
    INDEX idx_meeting_round_topic (meeting_round_id, topic_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- topic_recommendation_logs (AI 추천 호출 로그 - 일별 15회 제한용)
CREATE TABLE topic_recommendation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_topic_recommendation_log_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    INDEX idx_meeting_created_at (meeting_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
