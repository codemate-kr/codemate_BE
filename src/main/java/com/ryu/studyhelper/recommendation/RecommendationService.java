package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.notification.email.MailSendService;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.ProblemService;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.*;
import com.ryu.studyhelper.recommendation.dto.*;
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
     * 모든 활성 팀에 대해 일일 추천 생성 및 이메일 발송 (스케줄러용)
     */
    public void createDailyRecommendationsForAllTeams() {
        LocalDate today = LocalDate.now();
        log.info("일일 추천 생성 시작: {}", today);

        // 모든 활성 팀 조회 (팀원이 1명 이상이고 오늘이 추천 요일인 팀)
        DayOfWeek currentDayOfWeek = today.getDayOfWeek();
        List<Team> activeTeams = teamRepository.findAll().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .filter(team -> team.isRecommendationActive())
                .filter(team -> team.isRecommendationDay(currentDayOfWeek))
                .toList();

        int successCount = 0;
        int failCount = 0;

        for (Team team : activeTeams) {
            try {
                // 오늘 이미 일일 추천이 있는지 확인
//                log.info(String.valueOf(teamRecommendationRepository.existsByTeamAndRecommendationDate(team, today)));
                if (teamRecommendationRepository.existsByTeamAndRecommendationDate(team, today)) {
                    log.debug("팀 '{}'에 대해 오늘({}) 이미 추천 완료", team.getName(), today);
                    continue;
                }

                createDailyRecommendation(team, today);
                successCount++;
                log.info("팀 '{}' 일일 추천 생성 완료", team.getName());

            } catch (Exception e) {
                failCount++;
                log.error("팀 '{}' 일일 추천 생성 실패", team.getName(), e);
            }
        }

        log.info("일일 추천 배치 완료 - 대상 팀: {}개, 성공: {}개, 실패: {}개 ({})", 
                activeTeams.size(), successCount, failCount, currentDayOfWeek);
    }

    /**
     * 특정 팀에 대한 일일 추천 생성
     */
    private void createDailyRecommendation(Team team, LocalDate date) {
        // 1. 팀 추천 엔티티 생성
        TeamRecommendation recommendation = TeamRecommendation.createScheduledRecommendation(team, date);
        teamRecommendationRepository.save(recommendation);

        // 2. solved.ac에서 문제 추천 받기
        List<String> handles = teamMemberRepository.findHandlesByTeamId(team.getId());
        if (handles.isEmpty()) {
            log.warn("팀 '{}'에 인증된 핸들이 없습니다", team.getName());
            recommendation.markAsFailed();
            return;
        }

        List<ProblemInfo> problemInfos = problemService.recommend(handles, 3);

        // 3. 추천 문제들을 DB에 저장
        for (int i = 0; i < problemInfos.size(); i++) {
            ProblemInfo problemInfo = problemInfos.get(i);
            Problem problem = findOrCreateProblem(problemInfo);
            
            TeamRecommendationProblem recommendationProblem = 
                    TeamRecommendationProblem.create(problem, i + 1);
            recommendation.addProblem(recommendationProblem);
        }

        teamRecommendationRepository.save(recommendation);

        // 4. 이메일 발송
        try {
            List<String> memberEmails = teamMemberRepository.findEmailsByTeamId(team.getId());
            mailSendService.sendRecommendationEmail(recommendation, memberEmails);
            recommendation.markAsSent();
            log.info("팀 '{}' 추천 이메일 발송 완료", team.getName());
        } catch (Exception e) {
            recommendation.markAsFailed();
            log.error("팀 '{}' 이메일 발송 실패", team.getName(), e);
            throw e;
        }
    }

    /**
     * 수동 추천 생성 (팀장 요청)
     */
    public TeamRecommendationDetailResponse createManualRecommendation(Long teamId, int count) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 수동 추천 생성
        TeamRecommendation recommendation = TeamRecommendation.createManualRecommendation(team);
        teamRecommendationRepository.save(recommendation);

        // 문제 추천 및 저장
        List<String> handles = teamMemberRepository.findHandlesByTeamId(teamId);
        if (handles.isEmpty()) {
            throw new CustomException(CustomResponseStatus.TEAM_NOT_FOUND);
        }

        List<ProblemInfo> problemInfos = problemService.recommend(handles, count);
        for (int i = 0; i < problemInfos.size(); i++) {
            Problem problem = findOrCreateProblem(problemInfos.get(i));
            TeamRecommendationProblem recommendationProblem = 
                    TeamRecommendationProblem.create(problem, i + 1);
            recommendation.addProblem(recommendationProblem);
        }

        // 이메일 발송
        try {
            List<String> memberEmails = teamMemberRepository.findEmailsByTeamId(teamId);
            mailSendService.sendRecommendationEmail(recommendation, memberEmails);
            recommendation.markAsSent();
        } catch (Exception e) {
            recommendation.markAsFailed();
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
     * ProblemInfo로부터 Problem 엔티티 찾기 또는 생성
     */
    private Problem findOrCreateProblem(ProblemInfo problemInfo) {
        return problemRepository.findById(problemInfo.problemId())
                .orElseGet(() -> {
                    Problem problem = Problem.from(problemInfo);
                    return problemRepository.save(problem);
                });
    }
}