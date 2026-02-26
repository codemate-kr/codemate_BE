package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.team.domain.Squad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByTeamIdOrderByIdAsc(Long teamId);

    Optional<Squad> findByIdAndTeamId(Long squadId, Long teamId);

    Optional<Squad> findFirstByTeamIdAndIsDefaultTrueOrderByIdAsc(Long teamId);

    long countByTeamId(Long teamId);

    boolean existsByTeamIdAndName(Long teamId, String name);

    /**
     * 추천 활성 상태이고 특정 요일 비트가 설정된 스쿼드 목록 조회
     * 2차 배포 시 ScheduledRecommendationService의 getActiveTeams() 대체용
     */
    @Query(value = "SELECT * FROM squad WHERE recommendation_status = 'ACTIVE' AND (recommendation_days & :dayBit) > 0",
           nativeQuery = true)
    List<Squad> findActiveSquadsForDay(@Param("dayBit") int dayBit);
}
