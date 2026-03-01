-- 투표
CREATE TABLE votes (
    room_id             BIGINT UNSIGNED     NOT NULL,
    total_member_count  TINYINT UNSIGNED    NOT NULL COMMENT '투표 대상 총 인원수',
    opened_at           DATETIME(6)         NOT NULL COMMENT '투표 시작 시각',
    closed_at           DATETIME(6)         NULL COMMENT '투표 종료 시각 (NULL이면 진행 중)',
    agree_count         TINYINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '찬성 투표수',
    disagree_count      TINYINT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '반대 투표수',
    PRIMARY KEY (room_id),
    CONSTRAINT fk_votes_room FOREIGN KEY (room_id) REFERENCES chatting_rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 투표 참여 기록
CREATE TABLE vote_casts (
    room_id     BIGINT UNSIGNED     NOT NULL,
    user_id     BIGINT              NOT NULL,
    choice      VARCHAR(20)         NOT NULL COMMENT 'AGREE, DISAGREE',
    created_at  DATETIME(6)         NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_vote_casts_vote FOREIGN KEY (room_id) REFERENCES votes (room_id),
    CONSTRAINT fk_vote_casts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
