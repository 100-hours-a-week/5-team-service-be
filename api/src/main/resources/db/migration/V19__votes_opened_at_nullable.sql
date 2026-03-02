ALTER TABLE votes
    MODIFY COLUMN opened_at DATETIME(6) NULL COMMENT '투표 시작 시각 (NULL이면 아직 시작 안됨)';
