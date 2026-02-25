package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.team.domain.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * 모든 공개 팀 목록 조회
     * @return 공개 팀 목록
     */
    List<Team> findByIsPrivateFalse();

    /**
     * 기본 스쿼드 lazy 초기화 시 동시성 제어용 비관적 쓰기 락
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Team t WHERE t.id = :id")
    Optional<Team> findByIdWithPessimisticLock(@Param("id") Long id);
}
