-- user_accounts 테이블에 soft delete 지원 추가
-- 재가입 시 동일 provider_user_id로 신규 가입 가능하도록 unique 제약 변경

-- 1. deleted_at 컬럼 추가
ALTER TABLE user_accounts ADD COLUMN deleted_at DATETIME(6) NULL;

-- 2. Generated Column 추가 (active일 때만 provider_id 값, 삭제되면 NULL)
ALTER TABLE user_accounts 
ADD COLUMN active_provider_id VARCHAR(255) AS (
    CASE WHEN deleted_at IS NULL THEN provider_id ELSE NULL END
) STORED;

-- 3. 기존 unique 제약 제거
ALTER TABLE user_accounts DROP INDEX uk_provider_id;

-- 4. 새 unique 제약 추가 (활성 계정만 유니크)
ALTER TABLE user_accounts 
ADD CONSTRAINT uk_active_provider_id UNIQUE (provider, active_provider_id);

-- 5. deleted_at 인덱스 추가
CREATE INDEX idx_user_accounts_deleted_at ON user_accounts (deleted_at);
