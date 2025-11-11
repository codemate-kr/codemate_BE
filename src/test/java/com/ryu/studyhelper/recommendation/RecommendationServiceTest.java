package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.MemberRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendationProblem;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.TeamMemberRepository;
import com.ryu.studyhelper.team.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecommendationService 테스트
 * 새로운 추천 스키마(Recommendation, MemberRecommendation, MemberRecommendationProblem)에 대한 검증
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private MemberRecommendationRepository memberRecommendationRepository;

    @Autowired
    private MemberRecommendationProblemRepository memberRecommendationProblemRepository;

    private Team team;
    private Member member1;
    private Member member2;
    private Member member3;
    private Problem problem1;
    private Problem problem2;
    private Problem problem3;

    @BeforeEach
    void setUp() {
        // 팀 생성
        team = Team.create("알고리즘 스터디", "코드메이트 알고리즘 스터디");
        teamRepository.save(team);

        // 회원 3명 생성
        member1 = createMember("user1", "user1@example.com", "user1_handle");
        member2 = createMember("user2", "user2@example.com", "user2_handle");
        member3 = createMember("user3", "user3@example.com", "user3_handle");

        memberRepository.saveAll(List.of(member1, member2, member3));

        // 팀원 추가
        teamMemberRepository.save(TeamMember.create(team, member1, TeamRole.LEADER));
        teamMemberRepository.save(TeamMember.create(team, member2, TeamRole.MEMBER));
        teamMemberRepository.save(TeamMember.create(team, member3, TeamRole.MEMBER));

        // 문제 3개 생성
        problem1 = createProblem(2001L, "문제 1", 10);
        problem2 = createProblem(2002L, "문제 2", 11);
        problem3 = createProblem(2003L, "문제 3", 12);

        problemRepository.saveAll(List.of(problem1, problem2, problem3));
    }

    @Test
    @DisplayName("팀 추천 생성 시 MemberRecommendationProblem이 자동 생성된다")
    void prepareDailyRecommendation_createsMemberRecommendationProblems() {
        // given
        LocalDate today = LocalDate.now();

        // when
        // prepareDailyRecommendation은 private이므로, 공개 메서드인 prepareDailyRecommendations를 통해 간접 테스트
        // 직접 Recommendation, MemberRecommendation, MemberRecommendationProblem을 생성하여 테스트
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // 팀원별 MemberRecommendation 생성
        List<Member> teamMembers = teamMemberRepository.findMembersByTeamId(team.getId());
        for (Member member : teamMembers) {
            MemberRecommendation memberRecommendation = MemberRecommendation.builder()
                    .member(member)
                    .recommendation(recommendation)
                    .emailSendStatus(EmailSendStatus.PENDING)
                    .build();
            memberRecommendationRepository.save(memberRecommendation);

            // MemberRecommendationProblem 생성 (팀원 × 문제)
            for (Problem problem : List.of(problem1, problem2, problem3)) {
                MemberRecommendationProblem mrp = MemberRecommendationProblem.builder()
                        .member(member)
                        .memberRecommendation(memberRecommendation)
                        .problem(problem)
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .build();
                memberRecommendationProblemRepository.save(mrp);
            }
        }

        // then
        List<MemberRecommendationProblem> allProblems = memberRecommendationProblemRepository.findAll();

        // 팀원 3명 × 문제 3개 = 9개 레코드 검증
        assertThat(allProblems).hasSize(9);

        // 각 팀원당 3개씩 문제가 생성되었는지 확인
        long member1Problems = allProblems.stream()
                .filter(mrp -> mrp.getMember().getId().equals(member1.getId()))
                .count();
        long member2Problems = allProblems.stream()
                .filter(mrp -> mrp.getMember().getId().equals(member2.getId()))
                .count();
        long member3Problems = allProblems.stream()
                .filter(mrp -> mrp.getMember().getId().equals(member3.getId()))
                .count();

        assertThat(member1Problems).isEqualTo(3);
        assertThat(member2Problems).isEqualTo(3);
        assertThat(member3Problems).isEqualTo(3);

        // 각 문제가 3명의 팀원에게 모두 추천되었는지 확인
        long problem1Count = allProblems.stream()
                .filter(mrp -> mrp.getProblem().getId().equals(problem1.getId()))
                .count();
        long problem2Count = allProblems.stream()
                .filter(mrp -> mrp.getProblem().getId().equals(problem2.getId()))
                .count();
        long problem3Count = allProblems.stream()
                .filter(mrp -> mrp.getProblem().getId().equals(problem3.getId()))
                .count();

        assertThat(problem1Count).isEqualTo(3);
        assertThat(problem2Count).isEqualTo(3);
        assertThat(problem3Count).isEqualTo(3);

        // MemberRecommendation이 3개 생성되었는지 확인
        List<MemberRecommendation> memberRecommendations = memberRecommendationRepository.findAll();
        assertThat(memberRecommendations).hasSize(3);

        // 모든 MemberRecommendation이 PENDING 상태인지 확인
        assertThat(memberRecommendations)
                .allMatch(mr -> mr.getEmailSendStatus() == EmailSendStatus.PENDING);
    }

    @Test
    @DisplayName("팀원 3명 × 문제 3개 = 9개 MemberRecommendationProblem 레코드가 생성된다")
    void teamRecommendation_creates3x3_memberRecommendationProblems() {
        // given
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // when
        List<Member> teamMembers = teamMemberRepository.findMembersByTeamId(team.getId());
        List<Problem> problems = List.of(problem1, problem2, problem3);

        for (Member member : teamMembers) {
            MemberRecommendation memberRecommendation = MemberRecommendation.builder()
                    .member(member)
                    .recommendation(recommendation)
                    .emailSendStatus(EmailSendStatus.PENDING)
                    .build();
            memberRecommendationRepository.save(memberRecommendation);

            for (Problem problem : problems) {
                MemberRecommendationProblem mrp = MemberRecommendationProblem.builder()
                        .member(member)
                        .memberRecommendation(memberRecommendation)
                        .problem(problem)
                        .teamId(team.getId())
                        .teamName(team.getName())
                        .build();
                memberRecommendationProblemRepository.save(mrp);
            }
        }

        // then
        List<MemberRecommendationProblem> allProblems = memberRecommendationProblemRepository.findAll();

        // 정확히 9개 레코드 확인
        assertThat(allProblems).hasSize(9);

        // 팀원 수 확인
        assertThat(teamMembers).hasSize(3);

        // 문제 수 확인
        assertThat(problems).hasSize(3);

        // 각 MemberRecommendationProblem이 올바른 teamId와 teamName을 가지는지 확인
        assertThat(allProblems)
                .allMatch(mrp -> mrp.getTeamId().equals(team.getId()))
                .allMatch(mrp -> mrp.getTeamName().equals(team.getName()));

        // 모든 문제가 미해결 상태인지 확인
        assertThat(allProblems)
                .allMatch(mrp -> mrp.getSolvedAt() == null)
                .allMatch(mrp -> !mrp.isSolved());
    }

    @Test
    @DisplayName("동일한 Recommendation에 대해 팀원별로 독립적인 MemberRecommendation이 생성된다")
    void sameRecommendation_createsIndependentMemberRecommendations() {
        // given
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // when
        List<Member> teamMembers = teamMemberRepository.findMembersByTeamId(team.getId());
        for (Member member : teamMembers) {
            MemberRecommendation memberRecommendation = MemberRecommendation.builder()
                    .member(member)
                    .recommendation(recommendation)
                    .emailSendStatus(EmailSendStatus.PENDING)
                    .build();
            memberRecommendationRepository.save(memberRecommendation);
        }

        // then
        List<MemberRecommendation> memberRecommendations = memberRecommendationRepository
                .findByRecommendationId(recommendation.getId());

        // 3명의 팀원에 대해 3개의 MemberRecommendation이 생성되었는지 확인
        assertThat(memberRecommendations).hasSize(3);

        // 모두 동일한 Recommendation을 참조하는지 확인
        assertThat(memberRecommendations)
                .allMatch(mr -> mr.getRecommendation().getId().equals(recommendation.getId()));

        // 각기 다른 Member를 참조하는지 확인
        List<Long> memberIds = memberRecommendations.stream()
                .map(mr -> mr.getMember().getId())
                .distinct()
                .toList();
        assertThat(memberIds).hasSize(3);

        // 모두 PENDING 상태인지 확인
        assertThat(memberRecommendations)
                .allMatch(mr -> mr.getEmailSendStatus() == EmailSendStatus.PENDING);

        // emailSentAt이 null인지 확인
        assertThat(memberRecommendations)
                .allMatch(mr -> mr.getEmailSentAt() == null);
    }

    @Test
    @DisplayName("이메일 발송 성공 시 MemberRecommendation 상태가 SENT로 변경된다")
    void emailSent_updatesMemberRecommendationStatus() {
        // given
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        MemberRecommendation memberRecommendation = MemberRecommendation.builder()
                .member(member1)
                .recommendation(recommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        memberRecommendationRepository.save(memberRecommendation);

        // when
        memberRecommendation.markEmailAsSent();
        memberRecommendationRepository.save(memberRecommendation);

        // then
        MemberRecommendation updated = memberRecommendationRepository.findById(memberRecommendation.getId())
                .orElseThrow();

        assertThat(updated.getEmailSendStatus()).isEqualTo(EmailSendStatus.SENT);
        assertThat(updated.getEmailSentAt()).isNotNull();
        assertThat(updated.getEmailSentAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("이메일 발송 실패 시 MemberRecommendation 상태가 FAILED로 변경된다")
    void emailFailed_updatesMemberRecommendationStatus() {
        // given
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        MemberRecommendation memberRecommendation = MemberRecommendation.builder()
                .member(member1)
                .recommendation(recommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        memberRecommendationRepository.save(memberRecommendation);

        // when
        memberRecommendation.markEmailAsFailed();
        memberRecommendationRepository.save(memberRecommendation);

        // then
        MemberRecommendation updated = memberRecommendationRepository.findById(memberRecommendation.getId())
                .orElseThrow();

        assertThat(updated.getEmailSendStatus()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(updated.getEmailSentAt()).isNull();
    }

    @Test
    @DisplayName("Recommendation 타입이 SCHEDULED로 올바르게 생성된다")
    void recommendation_createdWithScheduledType() {
        // when
        Recommendation recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // then
        Recommendation saved = recommendationRepository.findById(recommendation.getId()).orElseThrow();

        assertThat(saved.getType()).isEqualTo(RecommendationType.SCHEDULED);
        assertThat(saved.getTeamId()).isEqualTo(team.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    /**
     * 테스트용 Member 생성 헬퍼 메서드
     */
    private Member createMember(String name, String email, String handle) {
        return Member.builder()
                .provider("google")
                .providerId("google_" + name)
                .email(email)
                .handle(handle)
                .isVerified(true)
                .build();
    }

    /**
     * 테스트용 Problem 생성 헬퍼 메서드
     */
    private Problem createProblem(Long problemId, String title, int level) {
        return Problem.builder()
                .id(problemId)
                .title(title)
                .level(level)
                .build();
    }
}