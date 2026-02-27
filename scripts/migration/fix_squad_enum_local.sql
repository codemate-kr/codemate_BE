-- 로컬 개발 환경 전용: squad 테이블 enum → varchar 변환
-- 프로덕션(V2 적용 환경)에서는 실행 불필요
USE codemate;

ALTER TABLE squad
    MODIFY COLUMN recommendation_status     varchar(16) NOT NULL DEFAULT 'INACTIVE',
    MODIFY COLUMN problem_difficulty_preset varchar(16) NULL;
