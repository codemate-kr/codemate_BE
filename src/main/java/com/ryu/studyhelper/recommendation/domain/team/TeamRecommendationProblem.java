package com.ryu.studyhelper.recommendation.domain.team;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.problem.domain.Problem;
import jakarta.persistence.*;
import lombok.*;

/**
 * 팀 추천에 포함된 문제들을 저장하는 엔티티
 * TeamRecommendation과 Problem 간의 다대다 관계를 중간 테이블로 구현
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "team_recommendation_problem")
public class TeamRecommendationProblem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_recommendation_id", nullable = false)
    @Setter
    private TeamRecommendation teamRecommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "recommendation_order")
    private Integer recommendationOrder;

    /**
     * 추천 문제 생성을 위한 팩토리 메서드
     */
    public static TeamRecommendationProblem create(Problem problem, Integer order) {
        return TeamRecommendationProblem.builder()
                .problem(problem)
                .recommendationOrder(order)
                .build();
    }
}