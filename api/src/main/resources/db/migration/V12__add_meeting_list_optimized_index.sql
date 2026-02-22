-- 모임 목록 조회 최적화 인덱스
-- 문제: 기존 idx_meeting_list(status, deleted_at, id)는 recruitment_deadline 조건을 커버하지 못함
-- 증상: 187만 행 테이블에서 recruitment_deadline >= CURDATE() 필터링 시 Full Table Scan 발생 (12~28초 소요)
-- 해결: recruitment_deadline을 포함한 복합 인덱스 추가

CREATE INDEX idx_meeting_list_v2
ON meetings (status, deleted_at, recruitment_deadline, id);