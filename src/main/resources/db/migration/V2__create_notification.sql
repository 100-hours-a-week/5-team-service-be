-- notification_types
CREATE TABLE notification_types (
                                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                    code VARCHAR(50) NOT NULL,
                                    title VARCHAR(80) NOT NULL,
                                    message_template VARCHAR(300) NOT NULL,
                                    link_template VARCHAR(255) NULL,
                                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                    deleted_at DATETIME(6) NULL,
                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_notification_types_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO notification_types (code, title, message_template, link_template) VALUES
                                                                                  ('ROUND_START_10M_BEFORE', '10분 후 토론이 시작돼요', '곧 화상 토론이 열려요. 지금 접속 환경을 한 번 확인해 주세요.', '/users/me/meetings/{meetingId}'),
                                                                                  ('BOOK_REVIEW_CHECKED', '독후감 검사가 완료됐어요', '검사 결과를 확인해 주세요.', '/users/me/meetings/{meetingId}'),
                                                                                  ('BOOK_REVIEW_DEADLINE_24H_BEFORE', '독후감 마감까지 하루 남았어요', '독후감을 제출해야 토론 모임에 참여할 수 있어요.', '/users/me/meetings/{meetingId}'),
                                                                                  ('BOOK_REVIEW_DEADLINE_30M_BEFORE', '독후감 마감까지 30분 남았어요', '지금 제출하면 토론에 참여할 수 있어요.', '/users/me/meetings/{meetingId}');

-- notifications
CREATE TABLE notifications (
                               id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               type_id BIGINT UNSIGNED NOT NULL,
                               title VARCHAR(80) NOT NULL,
                               message VARCHAR(300) NOT NULL,
                               link_path VARCHAR(255) NULL,
                               is_read TINYINT(1) NOT NULL DEFAULT 0,
                               read_at DATETIME(6) NULL,
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               deleted_at DATETIME(6) NULL,
                               PRIMARY KEY (id),
                               INDEX idx_notification_user_unread (user_id, is_read, created_at),
                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT fk_notifications_type FOREIGN KEY (type_id) REFERENCES notification_types(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_push_tokens
CREATE TABLE user_push_tokens (
                                  user_id BIGINT NOT NULL,
                                  platform VARCHAR(20) NOT NULL,
                                  provider VARCHAR(20) NOT NULL,
                                  token VARCHAR(512) NOT NULL,
                                  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  PRIMARY KEY (user_id),
                                  UNIQUE KEY uk_user_push_tokens_token (token),
                                  CONSTRAINT fk_user_push_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
