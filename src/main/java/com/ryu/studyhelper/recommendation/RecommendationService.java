package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.ProblemService;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendationProblem;
import com.ryu.studyhelper.recommendation.dto.response.TeamRecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.team.TeamMemberRepository;
import com.ryu.studyhelper.team.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
//    private final TeamRecommendationRepository teamRecommendationRepository;
    private final ProblemRepository problemRepository;
    private final ProblemService problemService;
    private final MailSendService mailSendService;

    // 신규 추천 시스템
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;



    /**
     * 수동 추천 생성 (팀장 요청)
     * 신규 스키마 사용: Recommendation → RecommendationProblem, MemberRecommendation
     * 즉시 이메일 발송
     */
    public TeamRecommendationDetailResponse createManualRecommendation(Long teamId, int count) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 1. Recommendation 생성 (MANUAL 타입)
        Recommendation recommendation = Recommendation.createManualRecommendation(teamId);
        recommendationRepository.save(recommendation);

        // 2. 문제 추천 및 RecommendationProblem 추가
        List<Problem> recommendedProblems = recommendProblemsForTeam(team, count);
        for (Problem problem : recommendedProblems) {
            RecommendationProblem rp = RecommendationProblem.create(problem);
            recommendation.addProblem(rp);
            recommendationProblemRepository.save(rp);
        }

        // 3. 팀원별 MemberRecommendation 생성 및 즉시 이메일 발송
        List<Member> teamMembers = teamMemberRepository.findMembersByTeamId(team.getId());
        for (Member member : teamMembers) {
            MemberRecommendation memberRecommendation = MemberRecommendation.create(member, recommendation, team);
            memberRecommendationRepository.save(memberRecommendation);

            // 4. 즉시 이메일 발송
            try {
                String memberEmail = member.getEmail();
                if (memberEmail == null || memberEmail.isBlank()) {
                    memberRecommendation.markEmailAsFailed();
                    memberRecommendationRepository.save(memberRecommendation);
                    log.warn("회원 ID {}에 이메일이 없습니다", member.getId());
                    continue;
                }

                mailSendService.sendMemberRecommendationEmail(memberRecommendation);
                memberRecommendation.markEmailAsSent();
                memberRecommendationRepository.save(memberRecommendation);
            } catch (Exception e) {
                memberRecommendation.markEmailAsFailed();
                memberRecommendationRepository.save(memberRecommendation);
                log.error("회원 ID {} 수동 추천 이메일 발송 실패", member.getId(), e);
            }
        }

        log.info("팀 '{}' 수동 추천 생성 완료 - 팀원: {}명, 문제: {}개",
                team.getName(), teamMembers.size(), recommendedProblems.size());

        // 6. 레거시 응답 형식 유지 (호환성)
        // TODO: 추후 응답 형식을 신규 스키마 기반으로 변경
        TeamRecommendation legacyResponse = TeamRecommendation.createManualRecommendation(team);
        for (int i = 0; i < recommendedProblems.size(); i++) {
            TeamRecommendationProblem trp = TeamRecommendationProblem.create(recommendedProblems.get(i), i + 1);
            legacyResponse.addProblem(trp);
        }
        legacyResponse.markAsSent();
        return TeamRecommendationDetailResponse.from(legacyResponse);
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
     * 신규 스키마 사용: Recommendation 조회
     */
    @Transactional(readOnly = true)
    public TeamRecommendationDetailResponse getTodayRecommendation(Long teamId) {

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59, 999999999);

        // 신규 스키마에서 오늘 추천 조회 (created_at 기준)
        List<Recommendation> todayRecommendations = recommendationRepository
                .findScheduledRecommendationsByCreatedAtBetween(startOfDay, endOfDay)
                .stream()
                .filter(rec -> rec.getTeamId().equals(teamId))
                .toList();

        if (todayRecommendations.isEmpty()) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_NOT_FOUND);
        }

        // 가장 최근 추천 반환
        Recommendation latestRecommendation = todayRecommendations.get(0);

        // 레거시 응답 형식으로 변환 (호환성 유지)
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        TeamRecommendation legacyResponse = TeamRecommendation.createScheduledRecommendation(team, today);
        List<Problem> problems = latestRecommendation.getProblems().stream()
                .map(RecommendationProblem::getProblem)
                .toList();

        for (int i = 0; i < problems.size(); i++) {
            TeamRecommendationProblem trp = TeamRecommendationProblem.create(problems.get(i), i + 1);
            legacyResponse.addProblem(trp);
        }
        legacyResponse.markAsSent();

        return TeamRecommendationDetailResponse.from(legacyResponse);
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
                // 신규 스키마 중복 체크 (created_at 기준)
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.atTime(23, 59, 59, 999999999);

                if (recommendationRepository.findByTeamIdAndCreatedAtBetweenAndType(
                        team.getId(), startOfDay, endOfDay, RecommendationType.SCHEDULED
                ).isPresent()) {
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
     * 신규 스키마 사용: Recommendation → RecommendationProblem, MemberRecommendation
     */
    private void prepareDailyRecommendation(Team team, LocalDate date) {
        // 1. Recommendation 생성
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // 2. 문제 추천 및 RecommendationProblem 추가
        List<Problem> recommendedProblems = recommendProblemsForTeam(team, 3);
        for (Problem problem : recommendedProblems) {
            RecommendationProblem rp = RecommendationProblem.create(problem);
            recommendation.addProblem(rp);
            recommendationProblemRepository.save(rp);
        }

        // 3. 팀원별 MemberRecommendation 생성
        List<Member> teamMembers = teamMemberRepository.findMembersByTeamId(team.getId());
        for (Member member : teamMembers) {
            MemberRecommendation memberRecommendation = MemberRecommendation.create(member, recommendation, team);
            memberRecommendationRepository.save(memberRecommendation);
        }

        log.info("팀 '{}' 신규 스키마 추천 생성 완료 - 팀원: {}명, 문제: {}개",
                team.getName(), teamMembers.size(), recommendedProblems.size());
    }

    /**
     * PENDING 상태의 추천들에 대해 이메일 발송 - 오전 스케줄러용
     * 신규 스키마 사용: MemberRecommendation 개별 발송
     */
    public void sendPendingRecommendationEmails() {
        LocalDate today = LocalDate.now();
        log.info("이메일 발송 배치 시작: {}", today);

        // 오늘 날짜의 PENDING 상태 개인 추천 조회
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59, 999999999);

        List<MemberRecommendation> pendingRecommendations = memberRecommendationRepository
                .findPendingRecommendationsByCreatedAtBetween(startOfDay, endOfDay, EmailSendStatus.PENDING);

        int successCount = 0;
        int failCount = 0;

        for (MemberRecommendation memberRecommendation : pendingRecommendations) {
            try {
                String memberEmail = memberRecommendation.getMember().getEmail();

                if (memberEmail == null || memberEmail.isBlank()) {
                    memberRecommendation.markEmailAsFailed();
                    memberRecommendationRepository.save(memberRecommendation);
                    log.warn("회원 ID {}에 이메일이 없습니다", memberRecommendation.getMember().getId());
                    failCount++;
                    continue;
                }

                // 개별 회원에게 이메일 발송
                mailSendService.sendMemberRecommendationEmail(memberRecommendation);
                memberRecommendation.markEmailAsSent();
                memberRecommendationRepository.save(memberRecommendation);

                successCount++;
                log.debug("회원 '{}' 이메일 발송 완료", memberRecommendation.getMember().getHandle());

            } catch (Exception e) {
                memberRecommendation.markEmailAsFailed();
                memberRecommendationRepository.save(memberRecommendation);

                failCount++;
                log.error("회원 ID {} 이메일 발송 실패",
                        memberRecommendation.getMember().getId(), e);
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
     * 팀에 문제 추천 (신규 스키마용)
     */
    private List<Problem> recommendProblemsForTeam(Team team, int count) {
        List<String> handles = teamMemberRepository.findHandlesByTeamId(team.getId());
        if (handles.isEmpty()) {
            log.warn("팀 '{}'에 인증된 핸들이 없습니다", team.getName());
            throw new IllegalStateException("인증된 핸들이 없습니다");
        }

        List<ProblemInfo> problemInfos = problemService.recommend(
                handles, count,
                team.getEffectiveMinProblemLevel(),
                team.getEffectiveMaxProblemLevel()
        );

        return problemInfos.stream()
                .map(this::findOrCreateProblem)
                .toList();
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

}