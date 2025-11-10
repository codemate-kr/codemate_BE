package com.ryu.studyhelper.recommendation.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 추천 배치를 저장하는 엔티티
 * 팀과 독립적으로 관리되며, 팀 삭제 시에도 데이터가 보존됩니다.
 *
 * AS-IS: TeamRecommendation이 Team과 강결합 (cascade delete)
 * TO-BE: Recommendation이 team_id를 애플리케이션 FK로만 참조 (DB FK 없음)
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

    /**
     * 추천 날짜 (어느 날 추천인지)
     */
    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    /**
     * 추천 타입 (SCHEDULED: 스케줄 자동 추천, MANUAL: 수동 추천)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16, nullable = false)
    private RecommendationType type;

    /**
     * 팀 ID (애플리케이션 FK만 사용, DB FK 제약조건 없음)
     * 팀 삭제 시에도 데이터 보존을 위해 nullable이며 DB FK를 사용하지 않습니다.
     */
    @Column(name = "team_id")
    private Long teamId;

    /**
     * 추천에 포함된 문제들 (1:N)
     */
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendationProblem> problems = new ArrayList<>();

    /**
     * 팀별 스케줄 추천 생성을 위한 팩토리 메서드
     *
     * @param teamId 팀 ID (DB FK 없음, 애플리케이션 레벨에서만 참조)
     * @param date 추천 날짜
     * @return 생성된 Recommendation 엔티티
     */
    public static Recommendation createScheduledRecommendation(Long teamId, LocalDate date) {
        return Recommendation.builder()
                .teamId(teamId)
                .type(RecommendationType.SCHEDULED)
                .recommendationDate(date)
                .build();
    }

    /**
     * 수동 추천 생성을 위한 팩토리 메서드
     *
     * @param teamId 팀 ID (DB FK 없음, 애플리케이션 레벨에서만 참조)
     * @return 생성된 Recommendation 엔티티
     */
    public static Recommendation createManualRecommendation(Long teamId) {
        return Recommendation.builder()
                .teamId(teamId)
                .type(RecommendationType.MANUAL)
                .recommendationDate(LocalDate.now())
                .build();
    }

    /**
     * 추천 문제 추가
     * 양방향 관계 설정을 위해 RecommendationProblem에도 this를 세팅합니다.
     *
     * @param problem 추가할 문제
     */
    public void addProblem(RecommendationProblem problem) {
        problems.add(problem);
        problem.setRecommendation(this);
    }
}