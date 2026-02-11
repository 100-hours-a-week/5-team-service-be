-- users
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       nickname VARCHAR(20) NOT NULL,
                       profile_image_path VARCHAR(512),
                       leader_intro VARCHAR(300),
                       member_intro VARCHAR(300),
                       onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
                       profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
                       deleted_at DATETIME(6),
                       role VARCHAR(20),
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       INDEX idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- reading_volumes
CREATE TABLE reading_volumes (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 name VARCHAR(50) NOT NULL,
                                 priority TINYINT NOT NULL,
                                 deleted_at DATETIME(6),
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                 INDEX idx_reading_volumes_deleted_priority (deleted_at, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- reading_purposes
CREATE TABLE reading_purposes (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  code VARCHAR(50) NOT NULL UNIQUE,
                                  name VARCHAR(50) NOT NULL,
                                  priority TINYINT NOT NULL,
                                  deleted_at DATETIME(6),
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  INDEX idx_reading_purpose_deleted_priority (deleted_at, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- reading_genres
CREATE TABLE reading_genres (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                code VARCHAR(20) NOT NULL UNIQUE,
                                name VARCHAR(50) NOT NULL,
                                priority TINYINT NOT NULL,
                                deleted_at DATETIME(6),
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                INDEX idx_reading_genre_deleted_priority (deleted_at, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- books
CREATE TABLE books (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       isbn13 VARCHAR(13) UNIQUE,
                       title VARCHAR(255) NOT NULL,
                       authors VARCHAR(255),
                       publisher VARCHAR(255),
                       thumbnail_url VARCHAR(512),
                       published_at DATE,
                       summary VARCHAR(2000),
                       deleted_at DATETIME(6),
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       INDEX idx_book_title_deleted_at (title, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- meetings
CREATE TABLE meetings (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          leader_user_id BIGINT NOT NULL,
                          reading_genre_id BIGINT NOT NULL,
                          leader_intro VARCHAR(300),
                          meeting_image_path VARCHAR(512) NOT NULL,
                          title VARCHAR(50) NOT NULL,
                          description VARCHAR(300) NOT NULL,
                          capacity TINYINT NOT NULL,
                          current_count TINYINT NOT NULL DEFAULT 1,
                          round_count TINYINT NOT NULL,
                          current_round TINYINT NOT NULL DEFAULT 1,
                          status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
                          day_of_week VARCHAR(3) NOT NULL,
                          start_time TIME NOT NULL,
                          duration_minutes SMALLINT NOT NULL DEFAULT 60,
                          first_round_at DATETIME(6) NOT NULL,
                          recruitment_deadline DATE NOT NULL,
                          deleted_at DATETIME(6),
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                          CONSTRAINT fk_meeting_leader_user FOREIGN KEY (leader_user_id) REFERENCES users(id),
                          CONSTRAINT fk_meeting_reading_genre FOREIGN KEY (reading_genre_id) REFERENCES reading_genres(id),
                          INDEX idx_meeting_list (status, deleted_at, id),
                          INDEX idx_meeting_genre_status (reading_genre_id, status, deleted_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- meeting_members
CREATE TABLE meeting_members (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 meeting_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,
                                 role VARCHAR(20) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 member_intro VARCHAR(300),
                                 approved_at DATETIME(6),
                                 rejected_at DATETIME(6),
                                 left_at DATETIME(6),
                                 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                 CONSTRAINT uk_meeting_member UNIQUE (meeting_id, user_id),
                                 CONSTRAINT fk_meeting_member_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id),
                                 CONSTRAINT fk_meeting_member_user FOREIGN KEY (user_id) REFERENCES users(id),
                                 INDEX idx_meeting_member_meeting_status_user (meeting_id, status, id),
                                 INDEX idx_meeting_member_user_status_meeting (user_id, status, meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- meeting_rounds
CREATE TABLE meeting_rounds (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                meeting_id BIGINT NOT NULL,
                                book_id BIGINT NOT NULL,
                                round_no TINYINT NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                meeting_link VARCHAR(1024),
                                start_at DATETIME(6) NOT NULL,
                                end_at DATETIME(6) NOT NULL,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                CONSTRAINT uk_meeting_round UNIQUE (meeting_id, round_no),
                                CONSTRAINT fk_meeting_round_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id),
                                CONSTRAINT fk_meeting_round_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_preferences
CREATE TABLE user_preferences (
                                  user_id BIGINT PRIMARY KEY,
                                  reading_volume_id BIGINT,
                                  notification_agreement TINYINT(1) NOT NULL DEFAULT 1,
                                  gender ENUM('MALE', 'FEMALE', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
                                  birth_year INT NOT NULL DEFAULT 0,
                                  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id),
                                  CONSTRAINT fk_user_preferences_reading_volume FOREIGN KEY (reading_volume_id) REFERENCES reading_volumes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_accounts
CREATE TABLE user_accounts (
                               user_id BIGINT PRIMARY KEY,
                               provider VARCHAR(20) NOT NULL,
                               provider_id VARCHAR(255) NOT NULL,
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                               CONSTRAINT uk_provider_id UNIQUE (provider, provider_id),
                               CONSTRAINT fk_user_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_stats
CREATE TABLE user_stats (
                            user_id BIGINT PRIMARY KEY,
                            total_exp BIGINT NOT NULL DEFAULT 0,
                            current_level INT NOT NULL DEFAULT 1,
                            consecutive_days INT NOT NULL DEFAULT 0,
                            last_attendance_date DATE,
                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_reading_purposes
CREATE TABLE user_reading_purposes (
                                       user_id BIGINT NOT NULL,
                                       reading_purpose_id BIGINT NOT NULL,
                                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                       PRIMARY KEY (user_id, reading_purpose_id),
                                       CONSTRAINT fk_urp_user FOREIGN KEY (user_id) REFERENCES users(id),
                                       CONSTRAINT fk_urp_reading_purpose FOREIGN KEY (reading_purpose_id) REFERENCES reading_purposes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_reading_genres
CREATE TABLE user_reading_genres (
                                     user_id BIGINT NOT NULL,
                                     reading_genre_id BIGINT NOT NULL,
                                     created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                     PRIMARY KEY (user_id, reading_genre_id),
                                     CONSTRAINT fk_urg_user FOREIGN KEY (user_id) REFERENCES users(id),
                                     CONSTRAINT fk_urg_reading_genre FOREIGN KEY (reading_genre_id) REFERENCES reading_genres(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- refresh_tokens
CREATE TABLE refresh_tokens (
                                token_id CHAR(13) PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked TINYINT(1) NOT NULL DEFAULT 0,
                                version BIGINT NOT NULL DEFAULT 0,
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- reading_volumes
INSERT INTO reading_volumes (code, name, priority, deleted_at) VALUES
                                                                   ('ONE_OR_LESS', '1권 이하', 1, NULL),
                                                                   ('TWO_TO_THREE', '2권 ~ 3권', 2, NULL),
                                                                   ('FOUR_TO_FIVE', '4권 ~ 5권', 3, NULL),
                                                                   ('FIVE_TO_SEVEN', '5권 ~ 7권', 4, NULL),
                                                                   ('EIGHT_OR_MORE', '8권 이상', 5, NULL);

-- reading_genres
INSERT INTO reading_genres (code, name, priority, deleted_at) VALUES
                                                                  ('NOVEL', '소설', 1, NULL),
                                                                  ('ECONOMY_BUSINESS', '경제/경영', 2, NULL),
                                                                  ('ESSAY', '에세이', 3, NULL),
                                                                  ('HUMANITIES_PHIL', '인문/철학', 4, NULL),
                                                                  ('SOCIETY_POLITICS', '사회/정치', 5, NULL),
                                                                  ('SELF_DEVELOPMENT', '자기계발', 6, NULL),
                                                                  ('SCIENCE_TECH', '과학/기술', 7, NULL),
                                                                  ('HISTORY', '역사', 8, NULL);

-- reading_purposes
INSERT INTO reading_purposes (code, name, priority, deleted_at) VALUES
                                                                    ('FINISH_TOGETHER', '함께 완독하기', 1, NULL),
                                                                    ('BUILD_READING_ROUTINE', '매주/매달 루틴 만들기', 2, NULL),
                                                                    ('SHARE_AND_DISCUSS', '감상 공유·토론하기', 3, NULL),
                                                                    ('KNOWLEDGE_GROWTH', '지식/실무 성장', 4, NULL),
                                                                    ('HEALING_AND_HOBBY', '힐링·취미로 즐기기', 5, NULL),
                                                                    ('MEET_NEW_PEOPLE', '새로운 사람들과 교류하기', 6, NULL);
