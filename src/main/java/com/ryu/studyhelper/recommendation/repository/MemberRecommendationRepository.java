package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MemberRecommendation 엔티티에 대한 Repository
 * 개인-추천 연결 및 이메일 발송 상태를 관리합니다.
 */
@Repository
public interface MemberRecommendationRepository extends JpaRepository<MemberRecommendation, Long> {

    /**
     * 특정 회원의 추천 이력 조회 (최신순)
     *
     * @param memberId 회원 ID
     * @return 개인 추천 목록
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "WHERE mr.member.id = :memberId " +
            "ORDER BY r.createdAt DESC")
    List<MemberRecommendation> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 특정 회원의 특정 추천 조회
     *
     * @param memberId 회원 ID
     * @param recommendationId 추천 ID
     * @return 개인 추천 (Optional)
     */
    Optional<MemberRecommendation> findByMemberIdAndRecommendationId(Long memberId, Long recommendationId);

    /**
     * 특정 이메일 발송 상태의 개인 추천 조회
     * 이메일 재시도 등에 사용됩니다.
     *
     * @param status 이메일 발송 상태
     * @return 개인 추천 목록
     */
    List<MemberRecommendation> findByEmailSendStatus(EmailSendStatus status);

    /**
     * 특정 날짜 범위의 PENDING 상태 개인 추천 조회
     * 스케줄러에서 이메일 발송 대상을 찾을 때 사용됩니다.
     * Service 레이어에서 날짜를 LocalDateTime 범위로 변환하여 전달
     *
     * @param startDateTime 날짜 범위 시작
     * @param endDateTime 날짜 범위 끝
     * @param status 이메일 발송 상태
     * @return 개인 추천 목록
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "JOIN FETCH mr.member m " +
            "WHERE r.createdAt BETWEEN :start AND :end " +
            "AND mr.emailSendStatus = :status")
    List<MemberRecommendation> findPendingRecommendationsByCreatedAtBetween(
            @Param("start") LocalDateTime startDateTime,
            @Param("end") LocalDateTime endDateTime,
            @Param("status") EmailSendStatus status
    );

    /**
     * 특정 추천에 연결된 모든 개인 추천 조회
     *
     * @param recommendationId 추천 ID
     * @return 개인 추천 목록
     */
    List<MemberRecommendation> findByRecommendationId(Long recommendationId);

    /**
     * 오늘 미션 사이클 이후 생성된 회원의 MemberRecommendation 조회 (v2 오늘의 문제용)
     * MemberRecommendation 기반으로 조회하므로 TeamMember JOIN 불필요
     *
     * @param memberId         회원 ID
     * @param missionCycleStart 오늘 미션 사이클 시작 시각 (오전 6시)
     * @return 오늘의 개인 추천 목록
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "WHERE mr.member.id = :memberId " +
            "AND r.createdAt >= :missionCycleStart")
    List<MemberRecommendation> findTodayByMemberId(
            @Param("memberId") Long memberId,
            @Param("missionCycleStart") LocalDateTime missionCycleStart
    );

    /**
     * 특정 회원의 MemberRecommendation에 해당 문제가 포함되어 있는지 확인
     * 스쿼드 간 인증 차단에 사용됩니다. 날짜 조건 없음.
     *
     * @param memberId  회원 ID
     * @param problemId 문제 ID
     * @return 포함 여부
     */
    @Query("SELECT COUNT(mr) > 0 FROM MemberRecommendation mr " +
            "JOIN mr.recommendation r " +
            "JOIN r.problems rp " +
            "WHERE mr.member.id = :memberId " +
            "AND rp.problem.id = :problemId")
    boolean existsByMemberIdAndRecommendedProblemId(
            @Param("memberId") Long memberId,
            @Param("problemId") Long problemId
    );

    /**
     * 현재 팀원의 MemberRecommendation 조회 (팀 활동 현황 V2 / 리더보드용)
     * TeamMember JOIN으로 현재 팀원 MR만 반환, recommendation·problems·member fetch join으로 N+1 방지
     * problems 컬렉션 JOIN FETCH로 인한 Cartesian product는 DISTINCT로 제거
     */
    @Query("SELECT DISTINCT mr FROM MemberRecommendation mr " +
            "JOIN TeamMember tm ON tm.member = mr.member AND tm.team.id = :teamId " +
            "JOIN FETCH mr.recommendation r " +
            "LEFT JOIN FETCH r.problems rp " +
            "LEFT JOIN FETCH rp.problem " +
            "JOIN FETCH mr.member " +
            "WHERE mr.teamId = :teamId " +
            "AND r.createdAt BETWEEN :start AND :end")
    List<MemberRecommendation> findByTeamIdAndCreatedAtBetween(
            @Param("teamId") Long teamId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}