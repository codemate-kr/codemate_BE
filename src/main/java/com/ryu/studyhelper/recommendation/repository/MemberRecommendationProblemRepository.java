package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendationProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MemberRecommendationProblem 엔티티에 대한 Repository
 * 개인별 추천 문제 추적 및 해결 인증을 관리합니다.
 */
@Repository
public interface MemberRecommendationProblemRepository extends JpaRepository<MemberRecommendationProblem, Long> {

    /**
     * TODO
     * 특정 개인 추천의 문제들을 순서대로 조회
     * 문제 순서는 RecommendationProblem.id 순서를 따릅니다.
     *
     * @param memberRecommendationId 개인 추천 ID
     * @return 문제 목록 (id 순서로 정렬)
     */
    @Query("SELECT mrp FROM MemberRecommendationProblem mrp " +
            "JOIN FETCH mrp.problem p " +
            "JOIN FETCH mrp.memberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "JOIN FETCH r.problems rp " +
            "WHERE mrp.memberRecommendation.id = :memberRecommendationId " +
            "ORDER BY rp.id ASC")
    List<MemberRecommendationProblem> findByMemberRecommendationIdOrderByRecommendationProblemId(
            @Param("memberRecommendationId") Long memberRecommendationId
    );

    /**
     * 특정 회원의 추천 문제들 조회 (최신순)
     *
     * @param memberId 회원 ID
     * @return 문제 목록
     */
    @Query("SELECT mrp FROM MemberRecommendationProblem mrp " +
            "JOIN FETCH mrp.problem p " +
            "WHERE mrp.member.id = :memberId " +
            "ORDER BY mrp.createdAt DESC")
    List<MemberRecommendationProblem> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 특정 문제를 추천받은 이력 조회 (최신순)
     * 중복 추천 확인에 사용됩니다.
     *
     * @param memberId 회원 ID
     * @param problemId 문제 ID
     * @return 문제 목록
     */
    List<MemberRecommendationProblem> findByMemberIdAndProblemIdOrderByCreatedAtDesc(
            Long memberId,
            Long problemId
    );

    /**
     * 특정 회원의 해결한 추천 문제 개수 조회
     *
     * @param memberId 회원 ID
     * @return 해결한 문제 개수
     */
    @Query("SELECT COUNT(mrp) FROM MemberRecommendationProblem mrp " +
            "WHERE mrp.member.id = :memberId " +
            "AND mrp.solvedAt IS NOT NULL")
    long countSolvedByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원의 전체 추천 문제 개수 조회
     *
     * @param memberId 회원 ID
     * @return 전체 추천 문제 개수
     */
    long countByMemberId(Long memberId);

    /**
     * 특정 팀의 특정 날짜 추천 문제들 조회
     * 팀 통계 산출에 사용됩니다.
     *
     * @param teamId 팀 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 문제 목록
     */
    @Query("SELECT mrp FROM MemberRecommendationProblem mrp " +
            "WHERE mrp.teamId = :teamId " +
            "AND mrp.createdAt BETWEEN :startDate AND :endDate")
    List<MemberRecommendationProblem> findByTeamIdAndDateRange(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 팀의 해결한 추천 문제 개수 조회 (기간 내)
     * 팀 통계 산출에 사용됩니다.
     *
     * @param teamId 팀 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해결한 문제 개수
     */
    @Query("SELECT COUNT(mrp) FROM MemberRecommendationProblem mrp " +
            "WHERE mrp.teamId = :teamId " +
            "AND mrp.createdAt BETWEEN :startDate AND :endDate " +
            "AND mrp.solvedAt IS NOT NULL")
    long countSolvedByTeamIdAndDateRange(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 팀의 전체 추천 문제 개수 조회 (기간 내)
     * 팀 통계 산출에 사용됩니다.
     *
     * @param teamId 팀 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 전체 추천 문제 개수
     */
    @Query("SELECT COUNT(mrp) FROM MemberRecommendationProblem mrp " +
            "WHERE mrp.teamId = :teamId " +
            "AND mrp.createdAt BETWEEN :startDate AND :endDate")
    long countByTeamIdAndDateRange(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}