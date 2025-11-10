package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
            "ORDER BY r.recommendationDate DESC")
    List<MemberRecommendation> findByMemberIdOrderByRecommendationDateDesc(@Param("memberId") Long memberId);

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
     * 특정 날짜의 PENDING 상태 개인 추천 조회
     * 스케줄러에서 이메일 발송 대상을 찾을 때 사용됩니다.
     *
     * @param date 추천 날짜
     * @param status 이메일 발송 상태
     * @return 개인 추천 목록
     */
    @Query("SELECT mr FROM MemberRecommendation mr " +
            "JOIN FETCH mr.recommendation r " +
            "JOIN FETCH mr.member m " +
            "WHERE r.recommendationDate = :date " +
            "AND mr.emailSendStatus = :status")
    List<MemberRecommendation> findPendingRecommendationsByDate(
            @Param("date") LocalDate date,
            @Param("status") EmailSendStatus status
    );

    /**
     * 특정 추천에 연결된 모든 개인 추천 조회
     *
     * @param recommendationId 추천 ID
     * @return 개인 추천 목록
     */
    List<MemberRecommendation> findByRecommendationId(Long recommendationId);
}