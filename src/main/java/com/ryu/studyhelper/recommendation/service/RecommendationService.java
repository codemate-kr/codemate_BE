package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.dto.projection.ProblemTagProjection;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.dto.internal.CreationResult;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;
import com.ryu.studyhelper.recommendation.dto.response.MyTodayProblemsResponse;
import com.ryu.studyhelper.recommendation.dto.response.RecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 추천 CRUD + 조회
 * 수동 추천 생성, 오늘의 추천 조회 등 컨트롤러/다른 도메인이 직접 호출하는 메서드
 */
@Service
@RequiredArgsConstructor
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
        Squad squad = squadRepository.findByIdAndTeamIdWithTeam(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        LocalDate missionDate = validateNoSquadRecommendationToday(teamId, squadId);

        CreationResult result =
                recommendationCreator.createForSquad(squad, RecommendationType.MANUAL, missionDate)
                        .orElseThrow(() -> new CustomException(CustomResponseStatus.NO_VERIFIED_HANDLE));

        recommendationEmailService.send(result.memberRecommendations());

        return RecommendationDetailResponse.from(result.recommendation(), squad.getTeam(), result.problems());
    }

    /**
     * 특정 팀/스쿼드의 오늘 추천 조회 (Optional 반환)
     * PENDING/FAILED → inProgress 응답, SUCCESS → 문제 목록 응답
     */
    @Transactional(readOnly = true)
    public Optional<TodayProblemResponse> findTodayRecommendationBySquad(Long teamId, Long squadId, Long memberId) {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);

        return recommendationRepository.findByTeamIdAndSquadIdAndDate(teamId, squadId, missionDate)
                .map(recommendation -> {
                    if (recommendation.getStatus() != RecommendationStatus.SUCCESS) {
                        return TodayProblemResponse.inProgress(recommendation);
                    }
                    return buildTodayProblemResponse(recommendation, memberId);
                });
    }

    /**
     * 로그인한 유저의 오늘 추천 문제 조회 (MemberRecommendation 기반)
     * MemberRecommendation은 SUCCESS 시에만 생성되므로 PENDING/FAILED는 조회되지 않음
     */
    @Transactional(readOnly = true)
    public MyTodayProblemsResponse getMyTodayProblemsV2(Long memberId) {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);

        List<MemberRecommendation> memberRecommendations =
                memberRecommendationRepository.findByMemberIdAndRecommendationDate(memberId, missionDate);

        List<MyTodayProblemsResponse.TeamTodayProblems> teamProblems = memberRecommendations.stream()
                .map(mr -> {
                    TodayProblemResponse todayProblemResponse =
                            buildTodayProblemResponse(mr.getRecommendation(), memberId);
                    return MyTodayProblemsResponse.TeamTodayProblems.from(
                            mr.getTeamId(), mr.getTeamName(), todayProblemResponse
                    );
                })
                .toList();

        int totalProblemCount = teamProblems.stream()
                .mapToInt(team -> team.problems().size())
                .sum();
        int solvedCount = teamProblems.stream()
                .flatMap(team -> team.problems().stream())
                .filter(problem -> Boolean.TRUE.equals(problem.isSolved()))
                .mapToInt(problem -> 1)
                .sum();

        return MyTodayProblemsResponse.from(teamProblems, totalProblemCount, solvedCount);
    }

    private TodayProblemResponse buildTodayProblemResponse(Recommendation recommendation, Long memberId) {
        List<ProblemWithSolvedStatusProjection> problemsWithStatus =
                recommendationProblemRepository.findProblemsWithSolvedStatus(recommendation.getId(), memberId);

        List<Long> problemIds = problemsWithStatus.stream()
                .map(ProblemWithSolvedStatusProjection::getProblemId)
                .toList();
        List<ProblemTagProjection> tagProjections = problemIds.isEmpty()
                ? List.of()
                : problemTagRepository.findTagsByProblemIds(problemIds);

        return TodayProblemResponse.from(recommendation, problemsWithStatus, tagProjections);
    }

    /**
     * 수동 추천 금지 시간 + 중복 체크.
     * PENDING/SUCCESS가 오늘 존재하면 예외. FAILED는 재시도 허용.
     *
     * @return 오늘의 미션 날짜
     */
    private LocalDate validateNoSquadRecommendationToday(Long teamId, Long squadId) {
        LocalTime now = LocalTime.now(clock);

        if (!now.isBefore(BLOCKED_START_TIME) && now.isBefore(BLOCKED_END_TIME)) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_BLOCKED_TIME);
        }

        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        recommendationRepository.findByTeamIdAndSquadIdAndDate(teamId, squadId, missionDate)
                .filter(rec -> rec.getStatus() != RecommendationStatus.FAILED)
                .ifPresent(rec -> {
                    throw new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                });

        return missionDate;
    }
}
