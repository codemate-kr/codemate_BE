package com.ryu.studyhelper.recommendation.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 추천 배치 (팀과 독립적 관리, soft delete)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "recommendation")
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    private RecommendationType type;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "squad_id")
    private Long squadId;

    @OneToMany(mappedBy = "recommendation")
    @Builder.Default
    private List<RecommendationProblem> problems = new ArrayList<>();

    public static Recommendation createScheduledRecommendation(Long teamId) {
        return Recommendation.builder()
                .teamId(teamId)
                .type(RecommendationType.SCHEDULED)
                .build();
    }

    public static Recommendation createManualRecommendation(Long teamId) {
        return Recommendation.builder()
                .teamId(teamId)
                .type(RecommendationType.MANUAL)
                .build();
    }

    public static Recommendation createManualRecommendationForSquad(Long teamId, Long squadId) {
        return Recommendation.builder()
                .teamId(teamId)
                .squadId(squadId)
                .type(RecommendationType.MANUAL)
                .build();
    }

    /**
     * 양방향 연관관계 편의 메서드
     */
    public void addProblem(RecommendationProblem problem) {
        problems.add(problem);
        problem.setRecommendation(this);
    }
}
