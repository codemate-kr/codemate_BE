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
    public void updateRecommendationDays(java.util.Set<RecommendationDayOfWeek> days) {
        this.recommendationDays = RecommendationDayOfWeek.toBitMask(days);
        // 추천이 설정되면 상태를 ACTIVE로, 설정이 없으면 INACTIVE로 변경
        if (this.recommendationDays > 0) {
            this.recommendationStatus = RecommendationStatus.ACTIVE;
        } else {
            this.recommendationStatus = RecommendationStatus.INACTIVE;
        }
    }

    /**
     * 현재 설정된 추천 요일들 조회
     */
    public java.util.Set<RecommendationDayOfWeek> getRecommendationDaysSet() {
        return RecommendationDayOfWeek.fromBitMask(this.recommendationDays);
    }

    /**
     * 추천 활성화 여부 확인
     */
    public boolean isRecommendationActive() {
        return this.recommendationStatus == RecommendationStatus.ACTIVE;
    }
}