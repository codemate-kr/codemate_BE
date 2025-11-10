package com.ryu.studyhelper.recommendation.domain.member;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 개인별 추천 문제 엔티티 (핵심 엔티티!)
 * 개인이 어떤 추천에서 어떤 문제를 받았는지, 풀었는지를 추적합니다.
 *
 * 핵심 역할:
 * 1. 개인별 추천 문제 독립적 추적 (중복 추천 허용)
 * 2. 문제 해결 인증 (solved_at으로 해결 여부 및 시각 기록)
 * 3. 팀 통계 쿼리 최적화 (team_id, problem_id denormalized)
 * 4. 팀 삭제 후에도 데이터 보존 (team_id는 FK 아님, team_name 저장)
 *
 * Denormalization 전략:
 * - problem_id: 팀 통계 쿼리 최적화 (1 JOIN 절약)
 * - team_id, team_name: 팀 삭제 후에도 데이터 표시 가능
 *
 * 중복 추천 허용 예시:
 * - 2024-01-15 추천: 문제 1001 (id=1, solved_at=NULL)
 * - 사용자가 1001 해결: (id=1, solved_at='2024-01-16 14:30')
 * - 2024-01-20 재추천: 문제 1001 (id=4, solved_at=NULL) ← 별도 레코드!
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member_recommendation_problem")
public class MemberRecommendationProblem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 회원 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 개인 추천 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_recommendation_id", nullable = false)
    @Setter
    private MemberRecommendation memberRecommendation;

    /**
     * 문제 참조 (FK)
     * Denormalized: 팀 통계 쿼리 성능 최적화 (1 JOIN 절약)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /**
     * 팀 ID (Denormalized, DB FK 없음)
     * 팀 삭제 후에도 데이터 보존을 위해 nullable이며 DB FK를 사용하지 않습니다.
     * 팀 통계 쿼리 성능 최적화에 사용됩니다.
     */
    @Column(name = "team_id")
    private Long teamId;

    /**
     * 팀 이름 (Denormalized)
     * 팀 삭제 후에도 "어느 팀에서 받은 추천인지" 표시하기 위해 저장합니다.
     */
    @Column(name = "team_name", length = 100)
    private String teamName;

    /**
     * 문제 해결 시각 (nullable)
     * - NULL: 미해결
     * - timestamp: 해결됨 (해결 시각 기록)
     *
     * 설계 노트:
     * - is_solved 컬럼은 불필요 (solved_at IS NULL로 미해결 판단 가능)
     * - 데이터 일관성 향상 (시간 정보만으로 상태 표현)
     * - 인덱스 효율 (solved_at 하나만 인덱싱)
     */
    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    /**
     * 개인 추천 문제 생성을 위한 팩토리 메서드
     *
     * @param member 회원
     * @param problem 문제
     * @param teamId 팀 ID (nullable, DB FK 없음)
     * @param teamName 팀 이름 (nullable, denormalized)
     * @return 생성된 MemberRecommendationProblem 엔티티
     */
    public static MemberRecommendationProblem create(
            Member member,
            Problem problem,
            Long teamId,
            String teamName
    ) {
        return MemberRecommendationProblem.builder()
                .member(member)
                .problem(problem)
                .teamId(teamId)
                .teamName(teamName)
                .solvedAt(null)  // 초기 상태는 미해결
                .build();
    }

    /**
     * 문제 해결 인증
     * 사용자가 문제를 풀었음을 기록합니다.
     */
    public void markAsSolved() {
        this.solvedAt = LocalDateTime.now();
    }

    /**
     * 문제 해결 여부 확인
     *
     * @return 해결 여부 (solved_at이 null이 아니면 해결됨)
     */
    public boolean isSolved() {
        return this.solvedAt != null;
    }
}