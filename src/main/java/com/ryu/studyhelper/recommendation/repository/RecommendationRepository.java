package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Recommendation 엔티티에 대한 Repository
 * 추천 배치 데이터를 관리합니다.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /**
     * 특정 팀의 특정 날짜 추천 조회
     *
     * @param teamId 팀 ID
     * @param recommendationDate 추천 날짜
     * @param type 추천 타입
     * @return 추천 배치 (Optional)
     */
    Optional<Recommendation> findByTeamIdAndRecommendationDateAndType(
            Long teamId,
            LocalDate recommendationDate,
            RecommendationType type
    );

    /**
     * 특정 팀의 추천 이력 조회 (최신순)
     *
     * @param teamId 팀 ID
     * @return 추천 배치 목록
     */
    List<Recommendation> findByTeamIdOrderByRecommendationDateDesc(Long teamId);

    /**
     * 특정 날짜의 모든 스케줄 추천 조회
     *
     * @param date 추천 날짜
     * @return 추천 배치 목록
     */
    @Query("SELECT r FROM Recommendation r WHERE r.recommendationDate = :date AND r.type = 'SCHEDULED'")
    List<Recommendation> findScheduledRecommendationsByDate(@Param("date") LocalDate date);
}