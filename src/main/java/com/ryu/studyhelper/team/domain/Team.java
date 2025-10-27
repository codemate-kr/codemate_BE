package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 팀 이름
    @Column(nullable = false)
    private String name;

    // 팀 설명
    private String description;

    // 추천 상태 (ACTIVE: 활성화, INACTIVE: 비활성화)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecommendationStatus recommendationStatus = RecommendationStatus.INACTIVE;

    // 추천받을 요일 비트마스크 (월요일=1, 화요일=2, 수요일=4, ...)
    @Column(name = "recommendation_days", nullable = false)
    @Builder.Default
    private Integer recommendationDays = RecommendationDayOfWeek.INACTIVE; // 기본값: 추천 비활성화

    // 추천 문제 난이도 프리셋 (기본값: NORMAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "problem_difficulty_preset")
    @Builder.Default
    private ProblemDifficultyPreset problemDifficultyPreset = ProblemDifficultyPreset.NORMAL;

    // 추천 문제 최소 난이도 (커스텀 모드일 때만 사용, 1~30)
    @Column(name = "min_problem_level")
    private Integer minProblemLevel;

    // 추천 문제 최대 난이도 (커스텀 모드일 때만 사용, 1~30)
    @Column(name = "max_problem_level")
    private Integer maxProblemLevel;

    // 팀원 목록 (일대다 관계)
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> teamMembers = new ArrayList<>();

    /**
     * 팀 생성을 위한 팩토리 메서드 (최초 생성시 추천 비활성화)
     */
    public static Team create(String name, String description) {
        return Team.builder()
                .name(name)
                .description(description)
                .recommendationStatus(RecommendationStatus.INACTIVE) // 기본값은 INACTIVE로 설정
                .recommendationDays(RecommendationDayOfWeek.INACTIVE) // 기본값: 추천 비활성화
                .build();
    }

    /**
     * 특정 요일에 추천을 받는지 확인
     */
    public boolean isRecommendationDay(java.time.DayOfWeek dayOfWeek) {
        return RecommendationDayOfWeek.isRecommendationDay(this.recommendationDays, dayOfWeek);
    }

    /**
     * 추천 요일 설정 업데이트
     */
    public void updateRecommendationDays(java.util.List<RecommendationDayOfWeek> days) {
        this.recommendationDays = RecommendationDayOfWeek.toBitMask(days);
        // 추천이 설정되면 상태를 ACTIVE로, 설정이 없으면 INACTIVE로 변경
        if (this.recommendationDays > 0) {
            this.recommendationStatus = RecommendationStatus.ACTIVE;
        } else {
            this.recommendationStatus = RecommendationStatus.INACTIVE;
        }
    }

    /**
     * 추천 난이도 설정 업데이트 (프리셋 방식)
     */
    public void updateProblemDifficultySettings(ProblemDifficultyPreset preset, Integer customMin, Integer customMax) {
        this.problemDifficultyPreset = preset;

        if (preset.isCustom()) {
            // 커스텀 모드: 사용자 정의 값 사용
            updateCustomLevelRange(customMin, customMax);
        } else {
            // 프리셋 모드: 커스텀 값 초기화
            this.minProblemLevel = null;
            this.maxProblemLevel = null;
        }
    }

    /**
     * 커스텀 난이도 범위 업데이트
     */
    private void updateCustomLevelRange(Integer minLevel, Integer maxLevel) {
        if (minLevel != null && minLevel >= 1 && minLevel <= 30) {
            this.minProblemLevel = minLevel;
        }
        if (maxLevel != null && maxLevel >= 1 && maxLevel <= 30) {
            // maxLevel은 minLevel보다 크거나 같아야 함
            if (this.minProblemLevel == null || maxLevel >= this.minProblemLevel) {
                this.maxProblemLevel = maxLevel;
            }
        }
    }

    /**
     * 현재 팀의 실제 최소 난이도 조회 (프리셋 또는 커스텀)
     */
    public Integer getEffectiveMinProblemLevel() {
        if (problemDifficultyPreset.isCustom()) {
            return minProblemLevel;
        }
        return problemDifficultyPreset.getMinLevel();
    }

    /**
     * 현재 팀의 실제 최대 난이도 조회 (프리셋 또는 커스텀)
     */
    public Integer getEffectiveMaxProblemLevel() {
        if (problemDifficultyPreset.isCustom()) {
            return maxProblemLevel;
        }
        return problemDifficultyPreset.getMaxLevel();
    }

    /**
     * 현재 설정된 추천 요일들 조회 (월요일부터 일요일까지 순서대로)
     */
    public java.util.List<RecommendationDayOfWeek> getRecommendationDaysList() {
        return RecommendationDayOfWeek.fromBitMask(this.recommendationDays);
    }

    /**
     * 추천 활성화 여부 확인
     */
    public boolean isRecommendationActive() {
        return this.recommendationStatus == RecommendationStatus.ACTIVE;
    }
}