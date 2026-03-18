-- 리뷰
CREATE TABLE reviews (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    reviewer_id       BIGINT          NOT NULL,
    meeting_round_id  BIGINT          NOT NULL,
    meeting_title     VARCHAR(50)     NOT NULL COMMENT '모임 제목 스냅샷',
    round_no          INT             NOT NULL COMMENT '회차 번호 스냅샷',
    book_title        VARCHAR(255)    NOT NULL COMMENT '책 제목 스냅샷',
    meeting_rating    DECIMAL(2,1)    NOT NULL COMMENT '모임 별점 (0.5~5.0)',
    leader_rating     DECIMAL(2,1)    NOT NULL COMMENT '모임장 별점 (0.5~5.0)',
    content           VARCHAR(200)    NULL COMMENT '리뷰 내용',
    best_member_id    BIGINT          NULL COMMENT '베스트 모임원 userId',
    deleted_at        DATETIME(6)     NULL,
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_meeting_round FOREIGN KEY (meeting_round_id) REFERENCES meeting_rounds (id),
    INDEX idx_reviews_reviewer_deleted (reviewer_id, deleted_at),
    INDEX idx_reviews_round_deleted (meeting_round_id, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 리뷰 이미지
CREATE TABLE review_images (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id     BIGINT          NOT NULL,
    image_path    VARCHAR(512)    NOT NULL COMMENT 'S3 key',
    image_order   INT             NOT NULL COMMENT '이미지 순서 (1~5)',
    created_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    INDEX idx_review_images_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
