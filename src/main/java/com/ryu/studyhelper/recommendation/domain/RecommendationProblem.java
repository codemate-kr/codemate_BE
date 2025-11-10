package com.ryu.studyhelper.recommendation.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.problem.domain.Problem;
import jakarta.persistence.*;
import lombok.*;

/**
 * 추천에 포함된 문제들을 저장하는 엔티티
 * Recommendation과 Problem 간의 다대다 관계를 중간 테이블로 구현
 *
 * AS-IS: TeamRecommendationProblem (recommendation_order 컬럼 존재)
 * TO-BE: RecommendationProblem (순서는 id 순서로 보장, recommendation_order 제거)
 *
 * 순서 보장 방법:
 * - 삽입 순서대로 id가 자동 증가하므로 id 순서 = 문제 순서
 * - Repository에서 ORDER BY id ASC로 정렬 필수
 * - 모든 팀원이 동일한 RecommendationProblem.id를 참조하므로 동일한 순서 보장
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "recommendation_problem")
public class RecommendationProblem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 추천 배치 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    @Setter
    private Recommendation recommendation;

    /**
     * 문제 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /**
     * 추천 문제 생성을 위한 팩토리 메서드
     * 문제 순서는 삽입 순서(id 순서)로 자동 보장됩니다.
     *
     * @param problem 추천할 문제
     * @return 생성된 RecommendationProblem 엔티티
     */
    public static RecommendationProblem create(Problem problem) {
        return RecommendationProblem.builder()
                .problem(problem)
                .build();
    }
}