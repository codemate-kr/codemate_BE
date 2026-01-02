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
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendationProblem;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;
import com.ryu.studyhelper.recommendation.dto.response.TeamRecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 추천 시스템 비즈니스 로직을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecommendationService {

    // 시간 관련 상수
    private static final LocalTime MISSION_RESET_TIME = LocalTime.of(6, 0);   // 미션 사이클 시작 (오전 6시)
    private static final LocalTime BLOCKED_START_TIME = LocalTime.of(5, 0);   // 수동 추천 금지 시작 (오전 5시)
    private static final LocalTime BLOCKED_END_TIME = LocalTime.of(7, 0);     // 수동 추천 금지 종료 (오전 7시)

    // 테스트 용이성을 위한 Clock 주입 (단위 테스트에서 시간 제어 가능)
    private final Clock clock;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProblemRepository problemRepository;
    private final ProblemService problemService;
    private final MailSendService mailSendService;

    // 신규 추천 시스템
    private final RecommendationRepository recommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;



    /**
     * 수동 추천 생성 (팀장 요청)
     * 즉시 이메일 발송
     * 다음 2가지 경우에만 추천 가능:
     * 1. 첫 팀 생성 후 아직 추천받은 적이 없는 경우
     * 2. 오늘 날짜 기준 추천이 발행되지 않은 경우
     *
     * @param teamId 팀 ID
     * @param count 추천 문제 개수 (null이면 팀 설정값 사용)
     */
    public TeamRecommendationDetailResponse createManualRecommendation(Long teamId, Integer count) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 오늘 이미 추천이 존재하는지 검증 (SCHEDULED, MANUAL 모두 포함)
        validateNoRecommendationToday(teamId);

        // count가 null이면 팀 설정값 사용
        int problemCount = (count != null) ? count : team.getProblemCount();

        // 1. Recommendation 생성 (MANUAL 타입)
        Recommendation recommendation = Recommendation.createManualRecommendation(teamId);
        recommendationRepository.save(recommendation);

        // 2. 문제 추천 및 RecommendationProblem 추가
        List<Problem> recommendedProblems = recommendProblemsForTeam(team, problemCount);
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

            // 4. 수동 추천은 항상 즉시 이메일 발송
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
     * 특정 팀의 오늘 추천 조회 (사용자별 해결 여부 포함)
     * @param teamId 팀 ID
     * @param memberId 회원 ID (nullable - 비로그인 시 null)
     * @throws CustomException 추천이 없는 경우 RECOMMENDATION_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public TodayProblemResponse getTodayRecommendation(Long teamId, Long memberId) {
        return findTodayRecommendation(teamId, memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.RECOMMENDATION_NOT_FOUND));
    }

    /**
     * 특정 팀의 오늘 추천 조회 (Optional 반환)
     * @param teamId 팀 ID
     * @param memberId 회원 ID (nullable - 비로그인 시 null)
     * @return 추천이 있으면 Optional.of(response), 없으면 Optional.empty()
     */
    @Transactional(readOnly = true)
    public Optional<TodayProblemResponse> findTodayRecommendation(Long teamId, Long memberId) {
        LocalDateTime missionCycleStart = getMissionCycleStart();

        return recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(teamId)
                .filter(recommendation -> !recommendation.getCreatedAt().isBefore(missionCycleStart))
                .map(recommendation -> {
                    List<ProblemWithSolvedStatusProjection> problemsWithStatus = recommendationProblemRepository
                            .findProblemsWithSolvedStatus(recommendation.getId(), memberId);
                    return TodayProblemResponse.from(recommendation, problemsWithStatus);
                });
    }


    /**
     * 문제 추천만 수행 (이메일 발송 X) - 오전 스케줄러용
     * 미션 사이클 기준(06:00~06:00)으로 중복 체크
     */
    public void prepareDailyRecommendations() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime missionCycleStart = getMissionCycleStart();
        log.info("문제 추천 준비 시작: {} (미션 사이클: {} 06:00 ~)", now.toLocalDate(), missionCycleStart.toLocalDate());

        List<Team> activeTeams = getActiveTeams(now.toLocalDate());
        int successCount = 0;
        int failCount = 0;

        for (Team team : activeTeams) {
            try {
                // 현재 미션 사이클 내 이미 추천이 있는지 체크 (SCHEDULED, MANUAL 모두 포함)
                if (recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                        team.getId(), missionCycleStart, now
                ).isPresent()) {
                    log.debug("팀 '{}'에 대해 현재 미션 사이클({})에 이미 추천 존재 - 스킵", team.getName(), missionCycleStart);
                    continue;
                }

                prepareDailyRecommendation(team);
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
    private void prepareDailyRecommendation(Team team) {
        // 1. Recommendation 생성
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // 2. 문제 추천 및 RecommendationProblem 추가
        List<Problem> recommendedProblems = recommendProblemsForTeam(team, team.getProblemCount());
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
     * 미션 사이클 기준(06:00~06:00)으로 조회
     */
    public void sendPendingRecommendationEmails() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime missionCycleStart = getMissionCycleStart();
        log.info("이메일 발송 배치 시작: {} (미션 사이클: {} 06:00 ~)", now.toLocalDate(), missionCycleStart.toLocalDate());

        // 현재 미션 사이클의 PENDING 상태 개인 추천 조회
        List<MemberRecommendation> pendingRecommendations = memberRecommendationRepository
                .findPendingRecommendationsByCreatedAtBetween(missionCycleStart, now, EmailSendStatus.PENDING);

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
     * - 기존 문제가 있으면 메타데이터 갱신 (solved.ac 최신 정보 반영)
     * - 새 문제면 생성
     */
    private Problem findOrCreateProblem(ProblemInfo problemInfo) {
        return problemRepository.findById(problemInfo.problemId())
                .map(existingProblem -> {
                    existingProblem.updateMetadata(
                            problemInfo.titleKo(),
                            problemInfo.level(),
                            problemInfo.acceptedUserCount(),
                            problemInfo.averageTries()
                    );
                    return existingProblem;
                })
                .orElseGet(() -> {
                    Problem problem = Problem.create(
                            problemInfo.problemId(),
                            problemInfo.titleKo(),
                            problemInfo.level(),
                            problemInfo.acceptedUserCount(),
                            problemInfo.averageTries()
                    );
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

    /**
     * 수동 추천 생성 가능 여부 검증
     * 1. 오전 5시~7시 사이: 생성 금지 (스케줄러 전환 구간)
     * 2. 현재 미션 사이클 내 추천이 있으면: 생성 금지
     *
     * @param teamId 팀 ID
     * @throws CustomException 생성 불가 시
     */
    private void validateNoRecommendationToday(Long teamId) {
        LocalTime now = LocalTime.now(clock);

        // 오전 5시~7시 사이는 수동 추천 금지
        if (!now.isBefore(BLOCKED_START_TIME) && now.isBefore(BLOCKED_END_TIME)) {
            throw new CustomException(CustomResponseStatus.RECOMMENDATION_BLOCKED_TIME);
        }

        // 현재 미션 사이클 시작 시간 이후(포함)에 생성된 추천이 있으면 금지
        // !isBefore 사용: 정확히 6시에 생성된 추천도 현재 사이클에 포함
        LocalDateTime missionCycleStart = getMissionCycleStart();
        recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(teamId)
                .filter(recommendation -> !recommendation.getCreatedAt().isBefore(missionCycleStart))
                .ifPresent(recommendation -> {
                    throw new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                });
    }

    /**
     * 현재 미션 사이클 시작 시간 계산
     * - 오전 6시 이후: 오늘 오전 6시
     * - 오전 6시 이전: 어제 오전 6시
     */
    private LocalDateTime getMissionCycleStart() {
        LocalDateTime now = LocalDateTime.now(clock);

        if (now.toLocalTime().isBefore(MISSION_RESET_TIME)) {
            return now.toLocalDate().minusDays(1).atTime(MISSION_RESET_TIME);
        }
        return now.toLocalDate().atTime(MISSION_RESET_TIME);
    }

}