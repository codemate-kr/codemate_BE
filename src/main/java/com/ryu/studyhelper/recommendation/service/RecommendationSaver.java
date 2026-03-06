package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Squad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 추천 저장 담당 — 각 메서드는 REQUIRES_NEW 트랜잭션으로 독립 커밋
 * RecommendationCreator / RecommendationBatchService에서 사용
 */
@Service
@RequiredArgsConstructor
class RecommendationSaver {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;

    /**
     * 배치용 PENDING INSERT.
     * UNIQUE 위반 시 DataIntegrityViolationException 그대로 전파 → 호출자가 스킵 처리.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Recommendation createPending(Squad squad, LocalDate date, RecommendationType type) {
        Recommendation pending = Recommendation.createPending(
                squad.getTeam().getId(), squad.getId(), type, date);
        return recommendationRepository.save(pending);
    }

    /**
     * 수동 추천용 레코드 조회/생성.
     * - 오늘 날짜 레코드가 없으면 PENDING 신규 생성
     * - FAILED면 PENDING으로 리셋 후 재사용
     * - SUCCESS/PENDING이면 중복 생성 방지를 위해 예외 발생
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Recommendation createOrResetPending(Squad squad, LocalDate date, RecommendationType type) {
        return recommendationRepository
                .findByTeamIdAndSquadIdAndDate(squad.getTeam().getId(), squad.getId(), date)
                .map(existing -> {
                    if (existing.getStatus() != RecommendationStatus.FAILED) {
                        throw new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    }
                    existing.retryAsPending();
                    return recommendationRepository.save(existing);
                })
                .orElseGet(() -> {
                    Recommendation pending = Recommendation.createPending(
                            squad.getTeam().getId(), squad.getId(), type, date);
                    return recommendationRepository.save(pending);
                });
    }

    /**
     * API 호출 성공 시 — 문제·멤버 저장 후 SUCCESS로 업데이트.
     * 저장된 MemberRecommendation 목록을 반환하여 호출자가 DB 재조회 없이 사용할 수 있도록 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<MemberRecommendation> saveSuccess(Recommendation rec, List<Problem> problems, List<Member> members, Squad squad) {
        for (Problem problem : problems) {
            recommendationProblemRepository.save(RecommendationProblem.create(problem, rec));
        }
        List<MemberRecommendation> memberRecommendations = members.stream()
                .map(member -> memberRecommendationRepository.save(
                        MemberRecommendation.createForSquad(member, rec, squad.getTeam(), squad.getId())))
                .toList();
        rec.markAsSuccess();
        recommendationRepository.save(rec);
        return memberRecommendations;
    }

    /**
     * API 호출 실패 시 — FAILED로 업데이트.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveFailed(Recommendation rec) {
        rec.markAsFailed();
        recommendationRepository.save(rec);
    }

    /**
     * FAILED → PENDING 전이 후 저장 (배치 재시도 진입점)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void resetToPending(Recommendation rec) {
        rec.retryAsPending();
        recommendationRepository.save(rec);
    }
}
