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
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.service.SquadService;
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
    private final TeamRepository teamRepository;
    private final SquadRepository squadRepository;
    private final SquadService squadService;
    private final TeamMemberRepository teamMemberRepository;
    private final ProblemTagRepository problemTagRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;
    private final RecommendationCreator recommendationCreator;
    private final RecommendationEmailService recommendationEmailService;

    /**
     * 수동 추천 생성 (팀장 요청)
     * 기본 스쿼드 기반으로 생성 (squadId 포함, team/squad read API 양쪽 조회 가능)
     * 즉시 이메일 발송
     */
    // TODO(#172): 2차 배포 시 제거 - 팀 기반 수동 추천, createManualRecommendationForSquad로 대체
    public RecommendationDetailResponse createManualRecommendation(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateNoRecommendationToday(teamId);

        // findDefaultSquad()가 lazy 초기화 포함 — 기존 팀에 squad 없어도 안전
        Squad defaultSquad = squadService.findDefaultSquad(teamId);

        Recommendation recommendation = recommendationCreator.createForSquad(defaultSquad, RecommendationType.MANUAL);

        // 즉시 이메일 발송
        List<MemberRecommendation> memberRecommendations =
                memberRecommendationRepository.findByRecommendationId(recommendation.getId());
        recommendationEmailService.send(memberRecommendations);

        Team team = defaultSquad.getTeam();
        List<Problem> problems = recommendation.getProblems().stream()
                .map(RecommendationProblem::getProblem)
                .toList();
        return RecommendationDetailResponse.from(recommendation, team, problems);
    }

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
     * 특정 팀의 오늘 추천 조회 (Optional 반환)
     */
    // TODO(#172): 2차 배포 시 제거 - 팀 기반 오늘 추천 조회, findTodayRecommendationBySquad로 대체
    @Transactional(readOnly = true)
    public Optional<TodayProblemResponse> findTodayRecommendation(Long teamId, Long memberId) {
        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);

        return recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(teamId)
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
     * 로그인한 유저가 속한 모든 팀의 오늘 추천 문제 조회
     * squadId가 설정된 경우 스쿼드 기반 조회, null이면 팀 기반 fallback (1차 배포 기간 한정)
     */
    // TODO(#172): 2차 배포 시 팀 기반 fallback 제거 (모든 TeamMember에 squadId 보장)
    @Transactional(readOnly = true)
    public MyTodayProblemsResponse getMyTodayProblems(Long memberId) {
        List<TeamMember> teamMemberships = teamMemberRepository.findByMemberId(memberId);

        List<MyTodayProblemsResponse.TeamTodayProblems> teamProblems = teamMemberships.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    Long squadId = tm.getSquadId();
                    // 1차 배포 기간: squadId 미배정(기존 팀) → 팀 기반 fallback
                    // TODO(#172): 2차 배포 시 else 분기 제거 — squadId != null 보장
                    Optional<TodayProblemResponse> todayProblem = squadId != null
                            ? findTodayRecommendationBySquad(team.getId(), squadId, memberId)
                            : findTodayRecommendation(team.getId(), memberId);
                    return todayProblem.map(tp -> MyTodayProblemsResponse.TeamTodayProblems.from(
                            team.getId(), team.getName(), tp
                    ));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
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

    // TODO(#172): 2차 배포 시 제거
    private void validateNoRecommendationToday(Long teamId) {
        LocalTime now = LocalTime.now(clock);

        if (!now.isBefore(BLOCKED_START_TIME) && now.isBefore(BLOCKED_END_TIME)) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_BLOCKED_TIME);
        }

        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);
        recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(teamId)
                .filter(recommendation -> !recommendation.getCreatedAt().isBefore(missionCycleStart))
                .ifPresent(recommendation -> {
                    throw new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                });
    }
}
