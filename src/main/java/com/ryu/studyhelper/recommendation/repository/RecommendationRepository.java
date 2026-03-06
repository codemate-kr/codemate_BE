package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    /**
     * 특정 팀/스쿼드의 오늘 추천 조회 (date 기반)
     * 수동 추천 중복 체크, 오늘의 추천 조회에 사용
     */
    Optional<Recommendation> findByTeamIdAndSquadIdAndDate(Long teamId, Long squadId, LocalDate date);

    /**
     * 특정 날짜 + 상태 목록에 해당하는 추천 조회
     * 재시도 배치 대상 조회에 사용
     */
    List<Recommendation> findByDateAndStatusIn(LocalDate date, List<RecommendationStatus> statuses);

    /**
     * 특정 팀의 추천 이력 조회 (최신순)
     */
    List<Recommendation> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * CAS(Compare-And-Swap) 방식 상태 업데이트
     * 현재 상태가 expectedStatus일 때만 newStatus로 전이 (동시성 안전)
     *
     * @return 업데이트된 행 수 (1: 성공, 0: 이미 다른 상태)
     */
    @Modifying
    @Query("UPDATE Recommendation r SET r.status = :newStatus WHERE r.id = :id AND r.status = :expectedStatus")
    int compareAndUpdateStatus(@Param("id") Long id,
                               @Param("newStatus") RecommendationStatus newStatus,
                               @Param("expectedStatus") RecommendationStatus expectedStatus);
}
