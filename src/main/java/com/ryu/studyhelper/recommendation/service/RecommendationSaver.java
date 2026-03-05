package com.ryu.studyhelper.recommendation.service;

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
     * 수동 추천용 PENDING 생성 또는 FAILED → PENDING 리셋.
     * 오늘 날짜에 FAILED 레코드가 있으면 재사용, 없으면 신규 INSERT.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Recommendation createOrResetPending(Squad squad, LocalDate date, RecommendationType type) {
        return recommendationRepository
                .findByTeamIdAndSquadIdAndDate(squad.getTeam().getId(), squad.getId(), date)
                .map(existing -> {
                    existing.updateStatus(RecommendationStatus.PENDING);
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
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveSuccess(Recommendation rec, List<Problem> problems, List<Member> members, Squad squad) {
        for (Problem problem : problems) {
            RecommendationProblem rp = RecommendationProblem.builder()
                    .recommendation(rec)
                    .problem(problem)
                    .build();
            recommendationProblemRepository.save(rp);
        }
        for (Member member : members) {
            MemberRecommendation mr = MemberRecommendation.createForSquad(
                    member, rec, squad.getTeam(), squad.getId());
            memberRecommendationRepository.save(mr);
        }
        rec.updateStatus(RecommendationStatus.SUCCESS);
        recommendationRepository.save(rec);
    }

    /**
     * API 호출 실패 시 — FAILED로 업데이트.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveFailed(Recommendation rec) {
        rec.updateStatus(RecommendationStatus.FAILED);
        recommendationRepository.save(rec);
    }
}
