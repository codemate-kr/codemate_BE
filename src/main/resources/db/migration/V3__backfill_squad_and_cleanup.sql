-- 1. 기존 팀에 기본 Squad 미생성분 보정
INSERT INTO squad (created_at, modified_at, team_id, name, description, is_default,
                   recommendation_status, recommendation_days, problem_difficulty_preset,
                   min_problem_level, max_problem_level, problem_count)
SELECT NOW(), NOW(), t.id, '스쿼드 A', NULL, 1,
       t.recommendation_status, t.recommendation_days, t.problem_difficulty_preset,
       t.min_problem_level, t.max_problem_level, t.problem_count
FROM team t
WHERE NOT EXISTS (SELECT 1 FROM squad s WHERE s.team_id = t.id AND s.is_default = 1);

-- 2. TeamIncludeTag → SquadIncludeTag 미복사분 보정
INSERT INTO squad_include_tag (squad_id, tag_key)
SELECT s.id, tit.tag_key
FROM team_include_tag tit
JOIN squad s ON s.team_id = tit.team_id AND s.is_default = 1
WHERE NOT EXISTS (
    SELECT 1 FROM squad_include_tag sit
    WHERE sit.squad_id = s.id AND sit.tag_key = tit.tag_key
);

-- 3. TeamMember.squad_id NULL인 것 보정
UPDATE team_member tm
JOIN squad s ON s.team_id = tm.team_id AND s.is_default = 1
SET tm.squad_id = s.id
WHERE tm.squad_id IS NULL;

-- 4. squad_id NOT NULL 제약 추가
ALTER TABLE team_member MODIFY COLUMN squad_id bigint NOT NULL;

-- 5. Team 추천 설정 컬럼 제거 (Squad로 이전 완료)
ALTER TABLE team
    DROP COLUMN recommendation_status,
    DROP COLUMN recommendation_days,
    DROP COLUMN problem_difficulty_preset,
    DROP COLUMN min_problem_level,
    DROP COLUMN max_problem_level,
    DROP COLUMN problem_count;

-- 6. TeamIncludeTag 테이블 제거
DROP TABLE IF EXISTS team_include_tag;
