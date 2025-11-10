package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.ProblemService;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.team.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendationProblem;
import com.ryu.studyhelper.recommendation.dto.*;
import com.ryu.studyhelper.recommendation.repository.TeamRecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.TeamRecommendationRepository;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.team.TeamMemberRepository;
import com.ryu.studyhelper.team.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 추천 시스템 비즈니스 로직을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRecommendationRepository teamRecommendationRepository;
    private final TeamRecommendationProblemRepository teamRecommendationProblemRepository;
    private final ProblemRepository problemRepository;
    private final ProblemService problemService;
    private final MailSendService mailSendService;



    /**
     * 수동 추천 생성 (팀장 요청)
     */
    public TeamRecommendationDetailResponse createManualRecommendation(Long teamId, int count) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        TeamRecommendation recommendation = TeamRecommendation.createManualRecommendation(team);
        teamRecommendationRepository.save(recommendation);

        addProblemsToRecommendation(recommendation, team, count);

        try {
            sendRecommendationEmailWithStatusUpdate(recommendation);
        } catch (Exception e) {
            log.error("수동 추천 이메일 발송 실패", e);
        }

        teamRecommendationRepository.save(recommendation);
        return TeamRecommendationDetailResponse.from(recommendation);
    }

    /**
     * 팀별 추천 이력 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<TeamRecommendationHistoryResponse> getTeamRecommendationHistory(Long teamId, Pageable pageable) {
        return teamRecommendationRepository.findByTeamIdOrderByRecommendationDateDesc(teamId, pageable)
                .map(TeamRecommendationHistoryResponse::from);
    }

    /**
     * 추천 상세 조회
     */
    @Transactional(readOnly = true)
    public TeamRecommendationDetailResponse getRecommendationDetail(Long recommendationId, Long memberId) {
        TeamRecommendation recommendation = teamRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.RECOMMENDATION_NOT_FOUND));

        // 팀 접근 권한 검증
        validateTeamAccess(recommendation.getTeam().getId(), memberId);

        return TeamRecommendationDetailResponse.from(recommendation);
    }

    /**
     * 오늘 추천 현황 조회
     */
    @Transactional(readOnly = true)
    public List<DailyRecommendationSummary> getTodayRecommendationSummary(Long memberId) {
        LocalDate today = LocalDate.now();
        return teamRecommendationRepository.findTodayRecommendationsByMemberId(memberId, today)
                .stream()
                .map(DailyRecommendationSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀 접근 권한 검증
     */
    public void validateTeamAccess(Long teamId, Long memberId) {
        boolean isMember = teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId);
        if (!isMember) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    /**
     * 특정 팀의 오늘 추천 조회
     */
    @Transactional(readOnly = true)
    public TeamRecommendationDetailResponse getTodayRecommendation(Long teamId, Long memberId) {
        // 팀 접근 권한 검증
        validateTeamAccess(teamId, memberId);

        LocalDate today = LocalDate.now();
        List<TeamRecommendation> todayRecommendations =
                teamRecommendationRepository.findByTeamIdAndRecommendationDateWithProblems(teamId, today);

        if (todayRecommendations.isEmpty()) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_NOT_FOUND);
        }

        // 가장 최근 추천 반환 (수동 추천이 있으면 우선, 없으면 자동 추천)
        TeamRecommendation latestRecommendation = todayRecommendations.get(0);
        return TeamRecommendationDetailResponse.from(latestRecommendation);
    }


    /**
     * 문제 추천만 수행 (이메일 발송 X) - 새벽 스케줄러용
     */
    public void prepareDailyRecommendations() {
        LocalDate today = LocalDate.now();
        log.info("문제 추천 준비 시작: {}", today);

        List<Team> activeTeams = getActiveTeams(today);
        int successCount = 0;
        int failCount = 0;

        for (Team team : activeTeams) {
            try {
                if (teamRecommendationRepository.existsByTeamAndRecommendationDate(team, today)) {
                    log.debug("팀 '{}'에 대해 오늘({}) 이미 추천 완료", team.getName(), today);
                    continue;
                }

                prepareDailyRecommendation(team, today);
                successCount++;
                log.info("팀 '{}' 문제 추천 완료", team.getName());

            } catch (Exception e) {
                failCount++;
                log.error("팀 '{}' 문제 추천 실패", team.getName(), e);
            }
        }

        log.info("문제 추천 배치 완료 - 대상: {}개, 성공: {}개, 실패: {}개",
                activeTeams.size(), successCount, failCount);
    }

    /**
     * 특정 팀에 대한 문제 추천 준비 (이메일 발송 X)
     */
    private void prepareDailyRecommendation(Team team, LocalDate date) {
        TeamRecommendation recommendation = TeamRecommendation.createScheduledRecommendation(team, date);
        teamRecommendationRepository.save(recommendation);

        addProblemsToRecommendation(recommendation, team, 3);

        teamRecommendationRepository.save(recommendation);
        log.info("팀 '{}' 문제 추천 DB 저장 완료", team.getName());
    }

    /**
     * PENDING 상태의 추천들에 대해 이메일 발송 - 오전 스케줄러용
     */
    public void sendPendingRecommendationEmails() {
        LocalDate today = LocalDate.now();
        log.info("이메일 발송 배치 시작: {}", today);

        // 오늘 날짜의 PENDING 상태 추천 조회
        List<TeamRecommendation> pendingRecommendations = teamRecommendationRepository
                .findByRecommendationDateAndType(today, RecommendationType.SCHEDULED)
                .stream()
                .filter(rec -> rec.getStatus() == RecommendationStatus.PENDING)
                .toList();

        int successCount = 0;
        int failCount = 0;

        for (TeamRecommendation recommendation : pendingRecommendations) {
            try {
                List<String> memberEmails = teamMemberRepository.findEmailsByTeamId(
                        recommendation.getTeam().getId()
                );

                if (memberEmails.isEmpty()) {
                    log.warn("팀 '{}'에 이메일이 없습니다", recommendation.getTeam().getName());
                    continue;
                }

                mailSendService.sendRecommendationEmail(recommendation, memberEmails);
                recommendation.markAsSent();
                teamRecommendationRepository.save(recommendation);

                successCount++;
                log.info("팀 '{}' 이메일 발송 완료", recommendation.getTeam().getName());

            } catch (Exception e) {
                recommendation.markAsFailed();
                teamRecommendationRepository.save(recommendation);

                failCount++;
                log.error("팀 '{}' 이메일 발송 실패", recommendation.getTeam().getName(), e);
            }
        }

        log.info("이메일 발송 배치 완료 - 대상: {}개, 성공: {}개, 실패: {}개",
                pendingRecommendations.size(), successCount, failCount);
    }

    /**
     * ProblemInfo로부터 Problem 엔티티 찾기 또는 생성
     */
    private Problem findOrCreateProblem(ProblemInfo problemInfo) {
        return problemRepository.findById(problemInfo.problemId())
                .orElseGet(() -> {
                    Problem problem = Problem.from(problemInfo);
                    return problemRepository.save(problem);
                });
    }

    /**
     * 활성 팀 조회 (팀원이 있고, 추천이 활성화되어 있으며, 오늘이 추천 요일인 팀)
     */
    private List<Team> getActiveTeams(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return teamRepository.findAll().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .filter(Team::isRecommendationActive)
                .filter(team -> team.isRecommendationDay(dayOfWeek))
                .toList();
    }

    /**
     * 추천에 문제 추가 (핸들 조회 → API 호출 → 문제 저장)
     */
    private void addProblemsToRecommendation(
            TeamRecommendation recommendation,
            Team team,
            int count
    ) {
        List<String> handles = teamMemberRepository.findHandlesByTeamId(team.getId());
        if (handles.isEmpty()) {
            log.warn("팀 '{}'에 인증된 핸들이 없습니다", team.getName());
            recommendation.markAsFailed();
            return;
        }

        List<ProblemInfo> problemInfos = problemService.recommend(
                handles, count,
                team.getEffectiveMinProblemLevel(),
                team.getEffectiveMaxProblemLevel()
        );

        for (int i = 0; i < problemInfos.size(); i++) {
            Problem problem = findOrCreateProblem(problemInfos.get(i));
            TeamRecommendationProblem recommendationProblem =
                    TeamRecommendationProblem.create(problem, i + 1);
            recommendation.addProblem(recommendationProblem);
        }
    }

    /**
     * 추천 이메일 발송
     */
    private void sendRecommendationEmailWithStatusUpdate(TeamRecommendation recommendation) {
        try {
            List<String> memberEmails = teamMemberRepository.findEmailsByTeamId(
                    recommendation.getTeam().getId()
            );
            mailSendService.sendRecommendationEmail(recommendation, memberEmails);
            recommendation.markAsSent();
            log.info("팀 '{}' 추천 이메일 발송 완료", recommendation.getTeam().getName());
        } catch (Exception e) {
            recommendation.markAsFailed();
            log.error("팀 '{}' 이메일 발송 실패", recommendation.getTeam().getName(), e);
            throw e;
        }
    }
}