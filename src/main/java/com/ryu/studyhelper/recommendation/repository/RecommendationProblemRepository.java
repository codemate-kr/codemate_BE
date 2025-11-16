package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RecommendationProblem 엔티티에 대한 Repository
 * 추천 배치에 포함된 문제들을 관리합니다.
 *
 * 중요: 문제 순서는 id 순서로 보장됩니다.
 * - Repository에서 ORDER BY id ASC 필수
 * - 삽입 순서 = id 순서 = 문제 순서
 */
@Repository
public interface RecommendationProblemRepository extends JpaRepository<RecommendationProblem, Long> {

    /**
     * 특정 추천의 문제들을 순서대로 조회
     * 문제 순서는 id 순서로 보장됩니다.
     *
     * @param recommendationId 추천 ID
     * @return 문제 목록 (id 순서로 정렬)
     */
    @Query("SELECT rp FROM RecommendationProblem rp " +
            "WHERE rp.recommendation.id = :recommendationId " +
            "ORDER BY rp.id ASC")
    List<RecommendationProblem> findByRecommendationIdOrderById(@Param("recommendationId") Long recommendationId);

    /**
     * 특정 추천의 문제들과 회원의 해결 여부를 함께 조회 (OUTER JOIN)
     * @param recommendationId 추천 ID
     * @param memberId 회원 ID (nullable - null이면 모두 미해결로 처리)
     * @return 문제와 해결 여부 목록
     */
    @Query("""
            SELECT p.id AS problemId,
                   p.title AS title,
                   p.titleKo AS titleKo,
                   p.level AS level,
                   p.acceptedUserCount AS acceptedUserCount,
                   p.averageTries AS averageTries,
                   CASE WHEN msp.id IS NOT NULL THEN true ELSE false END AS isSolved
            FROM RecommendationProblem rp
            JOIN rp.problem p
            LEFT JOIN MemberSolvedProblem msp ON msp.problem.id = p.id AND msp.member.id = :memberId
            WHERE rp.recommendation.id = :recommendationId
            ORDER BY rp.id ASC
            """)
    List<ProblemWithSolvedStatusProjection> findProblemsWithSolvedStatus(
            @Param("recommendationId") Long recommendationId,
            @Param("memberId") Long memberId
    );
}