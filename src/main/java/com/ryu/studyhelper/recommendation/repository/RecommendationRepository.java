package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Recommendation 엔티티에 대한 Repository
 * 추천 배치 데이터를 관리합니다.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /**
     * 특정 팀의 특정 날짜 범위 내 추천 조회
     */
    @Query("SELECT r FROM Recommendation r WHERE r.teamId = :teamId AND r.createdAt BETWEEN :start AND :end AND r.type = :type")
    Optional<Recommendation> findByTeamIdAndCreatedAtBetweenAndType(
            @Param("teamId") Long teamId,
            @Param("start") LocalDateTime startDateTime,
            @Param("end") LocalDateTime endDateTime,
            @Param("type") RecommendationType type
    );

    /**
     * 특정 팀의 추천 이력 조회 (최신순)
     */
    List<Recommendation> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * 특정 날짜 범위의 모든 추천 조회 (타입 무관)
     */
    List<Recommendation> findAllByCreatedAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    Optional<Recommendation> findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(Long teamId, Long squadId);

    /**
     * 특정 팀/스쿼드의 특정 날짜 범위 내 추천 조회 (타입 무관, 1개만)
     * 배치 스케줄러 스쿼드 기반 중복 체크 시 사용
     */
    Optional<Recommendation> findFirstByTeamIdAndSquadIdAndCreatedAtBetweenOrderById(
            Long teamId,
            Long squadId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
