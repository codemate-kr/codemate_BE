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
     * Service 레이어에서 날짜를 LocalDateTime 범위로 변환하여 전달
     *
     * @param teamId 팀 ID
     * @param startDateTime 날짜 범위 시작 (예: 2025-01-10 00:00:00)
     * @param endDateTime 날짜 범위 끝 (예: 2025-01-10 23:59:59.999999999)
     * @param type 추천 타입
     * @return 추천 배치 (Optional)
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
     *
     * @param teamId 팀 ID
     * @return 추천 배치 목록
     */
    List<Recommendation> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * 특정 날짜 범위의 모든 추천 조회 (타입 무관)
     * Service 레이어에서 날짜를 LocalDateTime 범위로 변환하여 전달
     *
     * @param startDateTime 날짜 범위 시작
     * @param endDateTime 날짜 범위 끝
     * @return 추천 배치 목록
     */
    List<Recommendation> findAllByCreatedAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 특정 팀의 가장 최근 추천 조회
     *
     * @param teamId 팀 ID
     * @return 가장 최근 추천 배치 (Optional)
     */
    Optional<Recommendation> findFirstByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * 특정 팀의 특정 날짜 범위 내 추천 조회 (타입 무관, 1개만)
     * 수동 추천 시 오늘 이미 추천이 있는지 검증할 때 사용
     * Spring Data JPA 메서드 네이밍 컨벤션 사용 (Hibernate 버전 독립적)
     *
     * @param teamId 팀 ID
     * @param startDateTime 날짜 범위 시작
     * @param endDateTime 날짜 범위 끝
     * @return 추천 (Optional)
     */
    Optional<Recommendation> findFirstByTeamIdAndCreatedAtBetweenOrderById(
            Long teamId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 특정 팀의 기간 내 모든 추천 조회 (문제 정보 포함, 최신순)
     * 팀 활동 현황 조회 시 사용
     *
     * @param teamId 팀 ID
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 추천 목록 (문제 정보 Fetch Join)
     */
    @Query("""
            SELECT DISTINCT r FROM Recommendation r
            LEFT JOIN FETCH r.problems rp
            LEFT JOIN FETCH rp.problem
            WHERE r.teamId = :teamId
              AND r.createdAt >= :start
              AND r.createdAt < :end
            ORDER BY r.createdAt DESC
            """)
    List<Recommendation> findByTeamIdAndCreatedAtBetweenWithProblems(
            @Param("teamId") Long teamId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}