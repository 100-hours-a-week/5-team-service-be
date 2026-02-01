-- user_meeting_recommendation
CREATE TABLE user_meeting_recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    `rank` INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_meeting (user_id, meeting_id),
    INDEX idx_user_week_rank (user_id, week_start_date, `rank`, meeting_id),
    CONSTRAINT fk_user_meeting_recommendation_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_meeting_recommendation_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
