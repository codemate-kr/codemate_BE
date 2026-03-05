package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.service.ProblemSyncService;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 스쿼드 1개에 대한 추천 생성 공통 로직
 * RecommendationService(수동)와 ScheduledRecommendationService(배치) 모두 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
class RecommendationCreator {

    private final TeamMemberRepository teamMemberRepository;
    private final SquadIncludeTagRepository squadIncludeTagRepository;
    private final SolvedAcClient solvedAcClient;
    private final ProblemSyncService problemSyncService;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;

    /**
     * 스쿼드 추천 생성.
     * 인증된 핸들이 없으면 {@link Optional#empty()} 를 반환(스킵).
     */
    Optional<Recommendation> createForSquad(Squad squad, RecommendationType type) {
        Long teamId = squad.getTeam().getId();
        List<String> handles = teamMemberRepository.findHandlesByTeamIdAndSquadId(teamId, squad.getId());
        if (handles.isEmpty()) {
            log.warn("스쿼드 '{}'에 인증된 핸들이 없습니다", squad.getName());
            return Optional.empty();
        }

        Recommendation base = (type == RecommendationType.MANUAL)
                ? Recommendation.createManualRecommendationForSquad(squad.getTeam().getId(), squad.getId())
                : Recommendation.createScheduledRecommendationForSquad(squad.getTeam().getId(), squad.getId());
        Recommendation recommendation = recommendationRepository.save(base);
        List<Problem> problems = recommendProblemsForSquad(squad, handles);
        for (Problem problem : problems) {
            RecommendationProblem rp = RecommendationProblem.create(problem);
            recommendation.addProblem(rp);
            recommendationProblemRepository.save(rp);
        }
        createMemberRecommendationsForSquad(recommendation, squad);

        log.info("추천 생성 완료 - 팀: {}, 스쿼드: {}, 타입: {}, 문제: {}개",
                squad.getTeam().getName(), squad.getName(), type, problems.size());

        return Optional.of(recommendation);
    }

    private void createMemberRecommendationsForSquad(Recommendation recommendation, Squad squad) {
        Team team = squad.getTeam();
        List<Member> squadMembers = teamMemberRepository.findMembersByTeamIdAndSquadId(team.getId(), squad.getId());
        for (Member member : squadMembers) {
            MemberRecommendation mr = MemberRecommendation.createForSquad(member, recommendation, team, squad.getId());
            memberRecommendationRepository.save(mr);
        }
    }

    private List<Problem> recommendProblemsForSquad(Squad squad, List<String> handles) {
        List<String> tagKeys = squadIncludeTagRepository.findTagKeysBySquadId(squad.getId());

        List<ProblemInfo> problemInfos = solvedAcClient.recommendUnsolvedProblems(
                handles,
                squad.getProblemCount(),
                squad.getEffectiveMinProblemLevel(),
                squad.getEffectiveMaxProblemLevel(),
                tagKeys
        );

        return problemSyncService.syncProblems(problemInfos);
    }
}
