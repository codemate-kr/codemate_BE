package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.recommendation.domain.TeamRecommendationProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 팀 추천 문제 데이터 접근을 위한 Repository
 */
@Repository
public interface TeamRecommendationProblemRepository extends JpaRepository<TeamRecommendationProblem, Long> {

    /**
     * 특정 추천에 포함된 문제들을 순서대로 조회
     */
    @Query("SELECT trp FROM TeamRecommendationProblem trp " +
           "WHERE trp.teamRecommendation.id = :recommendationId " +
           "ORDER BY trp.recommendationOrder ASC")
    List<TeamRecommendationProblem> findByTeamRecommendationIdOrderByOrder(
            @Param("recommendationId") Long recommendationId);
}