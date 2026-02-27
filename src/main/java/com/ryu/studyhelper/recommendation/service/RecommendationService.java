package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.dto.projection.ProblemTagProjection;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;
import com.ryu.studyhelper.recommendation.dto.response.MyTodayProblemsResponse;
import com.ryu.studyhelper.recommendation.dto.response.RecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 추천 CRUD + 조회
 * 수동 추천 생성, 오늘의 추천 조회 등 컨트롤러/다른 도메인이 직접 호출하는 메서드
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationService {

    private static final LocalTime BLOCKED_START_TIME = LocalTime.of(5, 0);
    private static final LocalTime BLOCKED_END_TIME = LocalTime.of(7, 0);

    private final Clock clock;
    private final SquadRepository squadRepository;
    private final ProblemTagRepository problemTagRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;
    private final RecommendationCreator recommendationCreator;
    private final RecommendationEmailService recommendationEmailService;

    /**
     * 스쿼드 수동 추천 생성 (팀장 요청)
     * 즉시 이메일 발송
     */
    public RecommendationDetailResponse createManualRecommendationForSquad(Long teamId, Long squadId) {
        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        validateNoSquadRecommendationToday(teamId, squadId);

        Recommendation recommendation = recommendationCreator.createForSquad(squad, RecommendationType.MANUAL);

        List<MemberRecommendation> memberRecommendations =
                memberRecommendationRepository.findByRecommendationId(recommendation.getId());
        recommendationEmailService.send(memberRecommendations);

        Team team = squad.getTeam();

        List<Problem> problems = recommendation.getProblems().stream()
                .map(RecommendationProblem::getProblem)
                .toList();
        return RecommendationDetailResponse.from(recommendation, team, problems);
    }

    /**
     * 특정 팀/스쿼드의 오늘 추천 조회 (Optional 반환)
     */
    @Transactional(readOnly = true)
    public Optional<TodayProblemResponse> findTodayRecommendationBySquad(Long teamId, Long squadId, Long memberId) {
        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);

        return recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(teamId, squadId)
                .filter(recommendation -> !recommendation.getCreatedAt().isBefore(missionCycleStart))
                .map(recommendation -> {
                    List<ProblemWithSolvedStatusProjection> problemsWithStatus = recommendationProblemRepository
                            .findProblemsWithSolvedStatus(recommendation.getId(), memberId);

                    List<Long> problemIds = problemsWithStatus.stream()
                            .map(ProblemWithSolvedStatusProjection::getProblemId)
                            .toList();
                    List<ProblemTagProjection> tagProjections = problemIds.isEmpty()
                            ? List.of()
                            : problemTagRepository.findTagsByProblemIds(problemIds);

                    return TodayProblemResponse.from(recommendation, problemsWithStatus, tagProjections);
                });
    }

    /**
     * 로그인한 유저의 오늘 추천 문제 조회 (MemberRecommendation 기반)
     * TeamMember가 아닌 MemberRecommendation 기반으로 조회하므로
     * 팀 탈퇴/해산 이후에도 오늘 받은 추천은 조회된다.
     */
    @Transactional(readOnly = true)
    public MyTodayProblemsResponse getMyTodayProblemsV2(Long memberId) {
        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);

        List<MemberRecommendation> memberRecommendations =
                memberRecommendationRepository.findTodayByMemberId(memberId, missionCycleStart);

        List<MyTodayProblemsResponse.TeamTodayProblems> teamProblems = memberRecommendations.stream()
                .map(mr -> {
                    Recommendation recommendation = mr.getRecommendation();

                    List<ProblemWithSolvedStatusProjection> problemsWithStatus =
                            recommendationProblemRepository.findProblemsWithSolvedStatus(recommendation.getId(), memberId);

                    List<Long> problemIds = problemsWithStatus.stream()
                            .map(ProblemWithSolvedStatusProjection::getProblemId)
                            .toList();
                    List<ProblemTagProjection> tagProjections = problemIds.isEmpty()
                            ? List.of()
                            : problemTagRepository.findTagsByProblemIds(problemIds);

                    TodayProblemResponse todayProblemResponse =
                            TodayProblemResponse.from(recommendation, problemsWithStatus, tagProjections);

                    return MyTodayProblemsResponse.TeamTodayProblems.from(
                            mr.getTeamId(), mr.getTeamName(), todayProblemResponse
                    );
                })
                .toList();

        return MyTodayProblemsResponse.from(teamProblems);
    }

    private void validateNoSquadRecommendationToday(Long teamId, Long squadId) {
        LocalTime now = LocalTime.now(clock);

        if (!now.isBefore(BLOCKED_START_TIME) && now.isBefore(BLOCKED_END_TIME)) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_BLOCKED_TIME);
        }

        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);
        recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(teamId, squadId)
                .filter(recommendation -> !recommendation.getCreatedAt().isBefore(missionCycleStart))
                .ifPresent(recommendation -> {
                    throw new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                });
    }
}
