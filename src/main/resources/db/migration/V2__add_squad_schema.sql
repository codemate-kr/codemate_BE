-- 팀 내 스쿼드 기능 1차 배포 스키마

CREATE TABLE squad
(
    id                        bigint                                     NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at                timestamp                                  NULL,
    deleted_at                datetime(6)                                NULL,
    modified_at               timestamp                                  NULL,
    team_id                   bigint                                     NOT NULL,
    name                      varchar(255)                               NOT NULL,
    description               varchar(255)                               NULL,
    is_default                tinyint(1) DEFAULT 0                       NOT NULL,
    recommendation_status     enum ('ACTIVE', 'INACTIVE')                NOT NULL,
    recommendation_days       int                                        NOT NULL,
    problem_difficulty_preset enum ('EASY', 'NORMAL', 'HARD', 'CUSTOM') NULL,
    min_problem_level         int                                        NULL,
    max_problem_level         int                                        NULL,
    problem_count             int        DEFAULT 3                       NOT NULL
);

CREATE INDEX idx_squad_team ON squad (team_id);
CREATE INDEX idx_squad_team_default ON squad (team_id, is_default);

CREATE TABLE squad_include_tag
(
    id       bigint      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    squad_id bigint      NOT NULL COMMENT 'Squad 참조',
    tag_key  varchar(64) NOT NULL COMMENT 'Tag 참조',
    CONSTRAINT uk_squad_include_tag UNIQUE (squad_id, tag_key)
) COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_squad_include_tag_squad ON squad_include_tag (squad_id);
CREATE INDEX idx_squad_include_tag_tag ON squad_include_tag (tag_key);

ALTER TABLE team_member
    ADD COLUMN squad_id bigint NULL;

ALTER TABLE recommendation
    ADD COLUMN squad_id bigint NULL;

ALTER TABLE member_recommendation
    ADD COLUMN squad_id bigint NULL;

CREATE INDEX idx_team_member_squad ON team_member (squad_id);
CREATE INDEX idx_recommendation_squad ON recommendation (squad_id);
CREATE INDEX idx_member_recommendation_squad ON member_recommendation (squad_id);

-- 기존 팀에 기본 스쿼드 백필 (신규 팀은 TeamService.create()에서 이미 생성됨)
INSERT INTO squad (created_at, modified_at, team_id, name, description, is_default,
                   recommendation_status, recommendation_days, problem_difficulty_preset,
                   min_problem_level, max_problem_level, problem_count)
SELECT NOW(), NOW(), t.id, '기본 스쿼드', '팀 기본 스쿼드', 1,
       t.recommendation_status, t.recommendation_days, t.problem_difficulty_preset,
       t.min_problem_level, t.max_problem_level, t.problem_count
FROM team t
WHERE NOT EXISTS (
    SELECT 1 FROM squad s WHERE s.team_id = t.id AND s.is_default = 1
);

-- 기존 팀 멤버를 기본 스쿼드에 배정
UPDATE team_member tm
    JOIN squad s ON s.team_id = tm.team_id AND s.is_default = 1
SET tm.squad_id = s.id
WHERE tm.squad_id IS NULL;

-- 배포 후 검증 쿼리 (0건이어야 정상)
-- SELECT t.id, t.name FROM team t WHERE (SELECT COUNT(*) FROM squad s WHERE s.team_id = t.id AND s.is_default = 1) != 1;
-- SELECT COUNT(*) FROM team_member WHERE squad_id IS NULL;
