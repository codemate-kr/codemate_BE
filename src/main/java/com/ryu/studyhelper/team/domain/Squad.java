package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "squad",
        indexes = {
                @Index(name = "idx_squad_team", columnList = "team_id"),
                @Index(name = "idx_squad_team_default", columnList = "team_id,is_default")
        }
)
public class Squad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "recommendation_status", nullable = false, length = 16)
    @Builder.Default
    private RecommendationStatus recommendationStatus = RecommendationStatus.INACTIVE;

    @Column(name = "recommendation_days", nullable = false)
    @Builder.Default
    private Integer recommendationDays = RecommendationDayOfWeek.INACTIVE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "problem_difficulty_preset", length = 16)
    @Builder.Default
    private ProblemDifficultyPreset problemDifficultyPreset = ProblemDifficultyPreset.NORMAL;

    @Column(name = "min_problem_level")
    private Integer minProblemLevel;

    @Column(name = "max_problem_level")
    private Integer maxProblemLevel;

    @Column(name = "problem_count", nullable = false)
    @Builder.Default
    private Integer problemCount = 3;

    public static Squad createDefault(Team team) {
        return Squad.builder()
                .team(team)
                .name("기본 스쿼드")
                .isDefault(true)
                .recommendationStatus(RecommendationStatus.INACTIVE)
                .recommendationDays(RecommendationDayOfWeek.INACTIVE)
                .problemDifficultyPreset(ProblemDifficultyPreset.NORMAL)
                .problemCount(3)
                .build();
    }

    public static Squad create(Team team, String name, String description) {
        return Squad.builder()
                .team(team)
                .name(name)
                .description(description)
                .isDefault(false)
                .recommendationStatus(RecommendationStatus.INACTIVE)
                .recommendationDays(RecommendationDayOfWeek.INACTIVE)
                .problemDifficultyPreset(ProblemDifficultyPreset.NORMAL)
                .problemCount(3)
                .build();
    }

    public void updateBasicInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void updateRecommendationDays(List<RecommendationDayOfWeek> days) {
        this.recommendationDays = RecommendationDayOfWeek.toBitMask(days);
        this.recommendationStatus = this.recommendationDays > 0
                ? RecommendationStatus.ACTIVE
                : RecommendationStatus.INACTIVE;
    }

    public void updateProblemDifficultySettings(ProblemDifficultyPreset preset, Integer customMin, Integer customMax) {
        if (preset.isCustom()) {
            // 검증 + 설정 성공한 경우에만 preset 변경
            if (updateCustomLevelRange(customMin, customMax)) {
                this.problemDifficultyPreset = preset;
            }
            return;
        }

        this.problemDifficultyPreset = preset;
        this.minProblemLevel = null;
        this.maxProblemLevel = null;
    }

    /** @return 유효한 범위면 true, 검증 실패면 false (preset은 변경되지 않음) */
    private boolean updateCustomLevelRange(Integer minLevel, Integer maxLevel) {
        if (minLevel == null || minLevel < 1 || minLevel > 30) return false;
        if (maxLevel == null || maxLevel < 1 || maxLevel > 30) return false;

        int newMin = minLevel;
        int newMax = maxLevel;
        if (newMin > newMax) {
            int tmp = newMin;
            newMin = newMax;
            newMax = tmp;
        }
        this.minProblemLevel = newMin;
        this.maxProblemLevel = newMax;
        return true;
    }

    public void updateProblemCount(Integer count) {
        if (count == null) return;
        this.problemCount = Math.max(1, Math.min(10, count));
    }

    public Integer getEffectiveMinProblemLevel() {
        if (problemDifficultyPreset.isCustom()) {
            return minProblemLevel;
        }
        return problemDifficultyPreset.getMinLevel();
    }

    public Integer getEffectiveMaxProblemLevel() {
        if (problemDifficultyPreset.isCustom()) {
            return maxProblemLevel;
        }
        return problemDifficultyPreset.getMaxLevel();
    }

    public List<RecommendationDayOfWeek> getRecommendationDaysList() {
        return RecommendationDayOfWeek.fromBitMask(this.recommendationDays);
    }

    public boolean isRecommendationActive() {
        return this.recommendationStatus == RecommendationStatus.ACTIVE;
    }

    public boolean isRecommendationDay(DayOfWeek dayOfWeek) {
        return RecommendationDayOfWeek.isRecommendationDay(this.recommendationDays, dayOfWeek);
    }
}
