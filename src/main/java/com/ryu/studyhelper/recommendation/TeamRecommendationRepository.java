package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.recommendation.domain.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.team.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 팀 추천 데이터 접근을 위한 Repository
 */
@Repository
public interface TeamRecommendationRepository extends JpaRepository<TeamRecommendation, Long> {

    /**
     * 특정 팀의 특정 날짜에 이미 추천이 존재하는지 확인
     */
    boolean existsByTeamAndRecommendationDate(Team team, LocalDate date);

    /**
     * 특정 팀의 특정 날짜와 타입에 해당하는 추천 조회
     */
    Optional<TeamRecommendation> findByTeamAndRecommendationDateAndType(
            Team team, LocalDate date, RecommendationType type);

    /**
     * 특정 팀의 추천 이력을 최신순으로 페이징 조회
     */
    @Query("SELECT tr FROM TeamRecommendation tr " +
           "WHERE tr.team.id = :teamId " +
           "ORDER BY tr.recommendationDate DESC, tr.createdAt DESC")
    Page<TeamRecommendation> findByTeamIdOrderByRecommendationDateDesc(
            @Param("teamId") Long teamId, Pageable pageable);

    /**
     * 특정 사용자가 속한 팀들의 오늘 추천 현황 조회
     */
    @Query("SELECT tr FROM TeamRecommendation tr " +
           "JOIN tr.team.teamMembers tm " +
           "WHERE tm.member.id = :memberId " +
           "AND tr.recommendationDate = :date " +
           "ORDER BY tr.team.name")
    List<TeamRecommendation> findTodayRecommendationsByMemberId(
            @Param("memberId") Long memberId, @Param("date") LocalDate date);

    /**
     * 특정 날짜의 모든 팀 추천 조회 (배치 처리용)
     */
    @Query("SELECT tr FROM TeamRecommendation tr " +
           "WHERE tr.recommendationDate = :date " +
           "AND tr.type = :type")
    List<TeamRecommendation> findByRecommendationDateAndType(
            @Param("date") LocalDate date, @Param("type") RecommendationType type);

    /**
     * 특정 팀의 오늘 추천 조회 (문제 정보 포함)
     */
    @Query("SELECT tr FROM TeamRecommendation tr " +
           "LEFT JOIN FETCH tr.problems trp " +
           "LEFT JOIN FETCH trp.problem " +
           "WHERE tr.team.id = :teamId " +
           "AND tr.recommendationDate = :date " +
           "ORDER BY tr.createdAt DESC")
    List<TeamRecommendation> findByTeamIdAndRecommendationDateWithProblems(
            @Param("teamId") Long teamId, @Param("date") LocalDate date);
}