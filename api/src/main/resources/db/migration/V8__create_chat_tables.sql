-- 채팅방
CREATE TABLE chatting_rooms (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    topic           VARCHAR(50)         NOT NULL COMMENT '채팅방 주제',
    description     VARCHAR(50)         NOT NULL COMMENT '한 줄 설명',
    capacity        TINYINT UNSIGNED    NOT NULL COMMENT '모집 인원수 (2, 4, 6)',
    duration        SMALLINT UNSIGNED   NOT NULL DEFAULT 30 COMMENT '진행 시간 (분)',
    status          VARCHAR(20)         NOT NULL COMMENT 'WAITING, CHATTING, ENDED, CANCELLED',
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_chatting_rooms_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 채팅방 라운드
CREATE TABLE room_rounds (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    room_id         BIGINT UNSIGNED     NOT NULL,
    round_number    TINYINT UNSIGNED    NOT NULL COMMENT '라운드 번호 (1~3)',
    started_at      DATETIME(6)         NOT NULL COMMENT '라운드 시작 시각',
    ended_at        DATETIME(6)         NULL COMMENT '라운드 종료 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_rounds_room_round (room_id, round_number),
    CONSTRAINT fk_room_rounds_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 채팅방 입장 멤버
CREATE TABLE chatting_room_members (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    room_id         BIGINT UNSIGNED     NOT NULL,
    user_id         BIGINT              NOT NULL,
    status          VARCHAR(20)         NOT NULL COMMENT 'WAITING, JOINED, DISCONNECTED, LEFT',
    role            VARCHAR(20)         NOT NULL COMMENT 'HOST, PARTICIPANT',
    position        VARCHAR(20)         NOT NULL COMMENT 'AGREE, DISAGREE',
    created_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_chatting_room_members_room_user (room_id, user_id),
    INDEX idx_chatting_room_members_room_status_user (room_id, status, user_id),
    CONSTRAINT fk_chatting_room_members_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id),
    CONSTRAINT fk_chatting_room_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 퀴즈
CREATE TABLE quizzes (
    room_id                 BIGINT UNSIGNED     NOT NULL,
    question                VARCHAR(50)         NOT NULL COMMENT '퀴즈 문제',
    correct_choice_number   TINYINT             NOT NULL COMMENT '정답 번호 (1~4)',
    PRIMARY KEY (room_id),
    CONSTRAINT fk_quizzes_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 퀴즈 선택지
CREATE TABLE quiz_choices (
    room_id         BIGINT UNSIGNED     NOT NULL,
    choice_number   TINYINT             NOT NULL COMMENT '선택지 번호 (1~4)',
    choice_text     VARCHAR(100)        NOT NULL COMMENT '선택지 텍스트',
    PRIMARY KEY (room_id, choice_number),
    CONSTRAINT fk_quiz_choices_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 메시지
CREATE TABLE messages (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    room_id             BIGINT UNSIGNED     NOT NULL COMMENT '방별 최근 메시지 조회용',
    round_id            BIGINT UNSIGNED     NOT NULL COMMENT '라운드별 메시지 분리용',
    sender_id           BIGINT              NOT NULL,
    client_message_id   VARCHAR(50)         NOT NULL COMMENT '클라이언트 메시지 ID (중복 방지)',
    message_type        VARCHAR(20)         NOT NULL COMMENT 'TEXT, FILE',
    text_message        VARCHAR(300)        NULL COMMENT '텍스트 메시지 본문',
    file_path           VARCHAR(512)        NULL COMMENT 'S3 파일 경로',
    created_at          DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_messages_room_sender_client (room_id, sender_id, client_message_id),
    INDEX idx_messages_room_id (room_id, id),
    INDEX idx_messages_round_id (round_id, id),
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id),
    CONSTRAINT fk_messages_round FOREIGN KEY (round_id) REFERENCES room_rounds (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
