package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRecommendationRepository extends JpaRepository<MemberRecommendation, Long> {

    /**
     * 특정 회원의 추천 이력 조회 (최신순)
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "WHERE mr.member.id = :memberId " +
            "ORDER BY r.createdAt DESC")
    List<MemberRecommendation> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 특정 회원의 특정 추천 조회
     */
    Optional<MemberRecommendation> findByMemberIdAndRecommendationId(Long memberId, Long recommendationId);

    /**
     * 이메일 발송 상태로 조회
     */
    List<MemberRecommendation> findByEmailSendStatus(EmailSendStatus status);

    /**
     * 특정 날짜 + 이메일 발송 상태의 개인 추천 조회
     * 이메일 발송 배치에서 오늘 대상 조회 시 사용
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "JOIN FETCH mr.member m " +
            "WHERE r.date = :date " +
            "AND mr.emailSendStatus = :status")
    List<MemberRecommendation> findByRecommendationDateAndEmailSendStatus(
            @Param("date") LocalDate date,
            @Param("status") EmailSendStatus status
    );

    /**
     * 이메일 상태 CAS 업데이트
     * UPDATE ... SET email_send_status = :newStatus WHERE id = :id AND email_send_status = :expectedStatus
     * 반환값이 1이면 선점 성공, 0이면 다른 워커가 이미 상태를 변경한 것
     */
    @Modifying
    @Transactional
    @Query("UPDATE MemberRecommendation mr SET mr.emailSendStatus = :newStatus WHERE mr.id = :id AND mr.emailSendStatus = :expectedStatus")
    int compareAndUpdateEmailSendStatus(@Param("id") Long id,
                                        @Param("newStatus") EmailSendStatus newStatus,
                                        @Param("expectedStatus") EmailSendStatus expectedStatus);

    /**
     * 특정 추천에 연결된 모든 개인 추천 조회
     */
    List<MemberRecommendation> findByRecommendationId(Long recommendationId);


    /**
     * 특정 날짜의 회원 추천 조회 (v2 오늘의 문제용)
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "WHERE mr.member.id = :memberId " +
            "AND r.date = :date")
    List<MemberRecommendation> findByMemberIdAndRecommendationDate(
            @Param("memberId") Long memberId,
            @Param("date") LocalDate date
    );

    /**
     * 특정 회원의 MemberRecommendation에 해당 문제가 포함되어 있는지 확인
     * 스쿼드 간 인증 차단에 사용됩니다. 날짜 조건 없음.
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
     * 현재 팀원의 MemberRecommendation 조회 (팀 활동 현황 / 리더보드용)
     * date 범위 기반 조회
     */
    @Query("SELECT DISTINCT mr FROM MemberRecommendation mr " +
            "JOIN TeamMember tm ON tm.member = mr.member AND tm.team.id = :teamId " +
            "JOIN FETCH mr.recommendation r " +
            "LEFT JOIN FETCH r.problems rp " +
            "LEFT JOIN FETCH rp.problem " +
            "JOIN FETCH mr.member " +
            "WHERE mr.teamId = :teamId " +
            "AND r.date BETWEEN :startDate AND :endDate")
    List<MemberRecommendation> findByTeamIdAndDateBetween(
            @Param("teamId") Long teamId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
