package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", nullable = false)
    @Builder.Default
    private RecommendationStatus recommendationStatus = RecommendationStatus.INACTIVE;

    @Column(name = "recommendation_days", nullable = false)
    @Builder.Default
    private Integer recommendationDays = RecommendationDayOfWeek.INACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "problem_difficulty_preset")
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
                .description("팀 기본 스쿼드")
                .isDefault(true)
                .recommendationStatus(team.getRecommendationStatus())
                .recommendationDays(team.getRecommendationDays())
                .problemDifficultyPreset(team.getProblemDifficultyPreset())
                .minProblemLevel(team.getMinProblemLevel())
                .maxProblemLevel(team.getMaxProblemLevel())
                .problemCount(team.getProblemCount())
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

    public void updateInfo(String name, String description) {
        updateBasicInfo(name, description);
    }

    public void updateRecommendationDays(List<RecommendationDayOfWeek> days) {
        this.recommendationDays = RecommendationDayOfWeek.toBitMask(days);
        this.recommendationStatus = this.recommendationDays > 0
                ? RecommendationStatus.ACTIVE
                : RecommendationStatus.INACTIVE;
    }

    public void updateProblemDifficultySettings(ProblemDifficultyPreset preset, Integer customMin, Integer customMax) {
        this.problemDifficultyPreset = preset;

        if (preset.isCustom()) {
            updateCustomLevelRange(customMin, customMax);
            return;
        }

        this.minProblemLevel = null;
        this.maxProblemLevel = null;
    }

    private void updateCustomLevelRange(Integer minLevel, Integer maxLevel) {
        if (minLevel != null && minLevel >= 1 && minLevel <= 30) {
            this.minProblemLevel = minLevel;
        }
        if (maxLevel != null && maxLevel >= 1 && maxLevel <= 30) {
            if (this.minProblemLevel == null || maxLevel >= this.minProblemLevel) {
                this.maxProblemLevel = maxLevel;
            }
        }
    }

    public void updateProblemCount(Integer count) {
        if (count != null && count >= 1 && count <= 10) {
            this.problemCount = count;
        }
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
