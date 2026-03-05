-- Step 1: type 컬럼 ENUM → varchar(16) 변환 (Hibernate 스키마 검증 통과)
ALTER TABLE recommendation
    MODIFY COLUMN type varchar(16) NOT NULL;

-- Step 2: 컬럼 추가 (date는 NULL 허용으로 시작)
ALTER TABLE recommendation
    ADD COLUMN date   date        NULL,
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'SUCCESS';

-- Step 2: 06:00 기준 날짜 백필 (06시 이전 생성 → 전날 미션)
UPDATE recommendation
SET date = IF(HOUR(created_at) < 6,
              DATE(created_at) - INTERVAL 1 DAY,
              DATE(created_at));

-- Step 3: NOT NULL 제약 + UNIQUE KEY 추가
--   (중복 제거는 배포 전 수동 실행 완료 전제)
ALTER TABLE recommendation
    MODIFY COLUMN date date NOT NULL,
    ADD UNIQUE KEY uq_recommendation_date_squad (date, squad_id);
