package com.ryu.studyhelper.recommendation.service;

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
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
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
    private final TeamMemberRepository teamMemberRepository;
    private final ProblemTagRepository problemTagRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;
    private final RecommendationCreator recommendationCreator;
    private final RecommendationEmailService recommendationEmailService;

    /**
     * 수동 추천 생성 (팀장 요청)
     * 즉시 이메일 발송
     */
    public RecommendationDetailResponse createManualRecommendation(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateNoRecommendationToday(teamId);

        Recommendation recommendation = recommendationCreator.create(team, RecommendationType.MANUAL);

        // 즉시 이메일 발송
        List<MemberRecommendation> memberRecommendations =
                memberRecommendationRepository.findByRecommendationId(recommendation.getId());
        recommendationEmailService.send(memberRecommendations);

        List<Problem> problems = recommendation.getProblems().stream()
                .map(RecommendationProblem::getProblem)
                .toList();
        return RecommendationDetailResponse.from(recommendation, team, problems);
    }

    /**
     * 특정 팀의 오늘 추천 조회 (Optional 반환)
     */
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
     * 로그인한 유저가 속한 모든 팀의 오늘 추천 문제 조회
     */
    @Transactional(readOnly = true)
    public MyTodayProblemsResponse getMyTodayProblems(Long memberId) {
        List<TeamMember> teamMemberships = teamMemberRepository.findByMemberId(memberId);

        List<MyTodayProblemsResponse.TeamTodayProblems> teamProblems = teamMemberships.stream()
                .map(tm -> {
                    Team team = tm.getTeam();
                    return findTodayRecommendation(team.getId(), memberId)
                            .map(todayProblem -> MyTodayProblemsResponse.TeamTodayProblems.from(
                                    team.getId(),
                                    team.getName(),
                                    todayProblem
                            ));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return MyTodayProblemsResponse.from(teamProblems);
    }

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
