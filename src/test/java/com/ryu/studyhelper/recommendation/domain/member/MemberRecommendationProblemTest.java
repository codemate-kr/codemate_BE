package com.ryu.studyhelper.recommendation.domain.member;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.MemberRepository;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MemberRecommendationProblem 엔티티 테스트
 * 팀 삭제, team_name 보존, 중복 추천 등 핵심 기능 검증
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("MemberRecommendationProblem 도메인 테스트")
class MemberRecommendationProblemTest {

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
    private Member member;
    private Problem problem;
    private Recommendation recommendation;
    private MemberRecommendation memberRecommendation;

    @BeforeEach
    void setUp() {
        // 팀 생성
        team = Team.create("알고리즘 스터디", "코드메이트 알고리즘 스터디");
        teamRepository.save(team);

        // 회원 생성
        member = Member.builder()
                .provider("google")
                .providerId("google_test")
                .email("test@example.com")
                .handle("test_handle")
                .isVerified(true)
                .build();
        memberRepository.save(member);

        // 팀원 추가
        teamMemberRepository.save(TeamMember.create(team, member, TeamRole.LEADER));

        // 문제 생성
        problem = Problem.builder()
                .id(1001L)
                .title("테스트 문제")
                .level(10)
                .build();
        problemRepository.save(problem);

        // 추천 생성
        recommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(recommendation);

        // 개인 추천 생성
        memberRecommendation = MemberRecommendation.builder()
                .member(member)
                .recommendation(recommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        memberRecommendationRepository.save(memberRecommendation);
    }

    @Test
    @DisplayName("MemberRecommendationProblem은 teamId와 teamName을 denormalization으로 저장한다")
    void memberRecommendationProblem_storesTeamIdAndNameAsDenormalization() {
        // given: MemberRecommendationProblem 생성
        MemberRecommendationProblem mrp = MemberRecommendationProblem.builder()
                .member(member)
                .memberRecommendation(memberRecommendation)
                .problem(problem)
                .teamId(team.getId())
                .teamName(team.getName())
                .build();
        memberRecommendationProblemRepository.save(mrp);

        // when: 저장된 데이터 조회
        MemberRecommendationProblem persisted = memberRecommendationProblemRepository
                .findById(mrp.getId())
                .orElseThrow();

        // then: teamId와 teamName이 제대로 저장됨
        assertThat(persisted).isNotNull();
        assertThat(persisted.getTeamId()).isEqualTo(team.getId());
        assertThat(persisted.getTeamName()).isEqualTo(team.getName());
        assertThat(persisted.getMember().getId()).isEqualTo(member.getId());
        assertThat(persisted.getProblem().getId()).isEqualTo(problem.getId());

        // MemberRecommendationProblem은 Team 엔티티를 FK로 참조하지 않음
        // → 팀과 독립적으로 데이터 보존 가능한 구조
        List<MemberRecommendationProblem> memberProblems = memberRecommendationProblemRepository
                .findByMemberIdOrderByCreatedAtDesc(member.getId());

        assertThat(memberProblems).hasSize(1);
        assertThat(memberProblems.get(0).getTeamId()).isEqualTo(team.getId());
    }

    @Test
    @DisplayName("teamName은 저장 시점의 팀 이름을 저장한다")
    void teamName_storesTeamNameAtCreationTime() {
        // given: 특정 이름으로 MemberRecommendationProblem 생성
        String customTeamName = "커스텀 팀 이름";

        MemberRecommendationProblem mrp = MemberRecommendationProblem.builder()
                .member(member)
                .memberRecommendation(memberRecommendation)
                .problem(problem)
                .teamId(team.getId())
                .teamName(customTeamName)  // 저장 시점의 팀 이름
                .build();
        memberRecommendationProblemRepository.save(mrp);

        // when: 저장된 데이터 조회
        MemberRecommendationProblem persisted = memberRecommendationProblemRepository
                .findById(mrp.getId())
                .orElseThrow();

        // then: teamName이 저장 시점의 값으로 유지됨
        assertThat(persisted.getTeamName()).isEqualTo(customTeamName);
        assertThat(persisted.getTeamId()).isEqualTo(team.getId());

        // denormalization 덕분에 Team 엔티티 없이도 팀 정보 확인 가능
        // "어느 팀에서 받은 추천인지" 이력 보존
    }

    @Test
    @DisplayName("중복 추천 시 별도의 MemberRecommendationProblem 레코드가 생성된다")
    void duplicateRecommendation_createsSeperateRecords() {
        // given: 첫 번째 추천에서 문제 1001 추천
        MemberRecommendationProblem firstRecommendation = MemberRecommendationProblem.builder()
                .member(member)
                .memberRecommendation(memberRecommendation)
                .problem(problem)
                .teamId(team.getId())
                .teamName(team.getName())
                .solvedAt(null)  // 미해결
                .build();
        memberRecommendationProblemRepository.save(firstRecommendation);

        // 사용자가 문제를 해결
        firstRecommendation.markAsSolved();
        memberRecommendationProblemRepository.save(firstRecommendation);

        // when: 두 번째 추천에서 동일한 문제 1001 재추천
        Recommendation secondRecommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(secondRecommendation);

        MemberRecommendation secondMemberRecommendation = MemberRecommendation.builder()
                .member(member)
                .recommendation(secondRecommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        memberRecommendationRepository.save(secondMemberRecommendation);

        MemberRecommendationProblem duplicateRecommendation = MemberRecommendationProblem.builder()
                .member(member)
                .memberRecommendation(secondMemberRecommendation)
                .problem(problem)  // 동일한 문제!
                .teamId(team.getId())
                .teamName(team.getName())
                .solvedAt(null)  // 재추천은 미해결 상태로 시작
                .build();
        memberRecommendationProblemRepository.save(duplicateRecommendation);

        // then: 동일한 문제에 대해 2개의 별도 레코드가 존재
        List<MemberRecommendationProblem> problemHistory = memberRecommendationProblemRepository
                .findByMemberIdAndProblemIdOrderByCreatedAtDesc(member.getId(), problem.getId());

        assertThat(problemHistory).hasSize(2);

        // 첫 번째 추천은 해결됨
        MemberRecommendationProblem first = problemHistory.stream()
                .filter(mrp -> mrp.getId().equals(firstRecommendation.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(first.isSolved()).isTrue();
        assertThat(first.getSolvedAt()).isNotNull();

        // 두 번째 추천(재추천)은 미해결
        MemberRecommendationProblem second = problemHistory.stream()
                .filter(mrp -> mrp.getId().equals(duplicateRecommendation.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(second.isSolved()).isFalse();
        assertThat(second.getSolvedAt()).isNull();

        // 각각 다른 MemberRecommendation을 참조
        assertThat(first.getMemberRecommendation().getId())
                .isNotEqualTo(second.getMemberRecommendation().getId());

        // 각각 다른 Recommendation을 참조
        assertThat(first.getMemberRecommendation().getRecommendation().getId())
                .isNotEqualTo(second.getMemberRecommendation().getRecommendation().getId());
    }

    @Test
    @DisplayName("중복 추천에서 이전 추천 해결 여부는 새 추천에 영향을 주지 않는다")
    void duplicateRecommendation_independentSolvedStatus() {
        // given: 첫 번째 추천
        MemberRecommendationProblem firstRec = createMemberRecommendationProblem(memberRecommendation, problem);

        // 사용자가 첫 번째 추천 문제를 해결
        firstRec.markAsSolved();
        memberRecommendationProblemRepository.save(firstRec);
        LocalDateTime firstSolvedAt = firstRec.getSolvedAt();

        // when: 동일한 문제를 재추천
        Recommendation newRecommendation = Recommendation.createScheduledRecommendation(team.getId());
        recommendationRepository.save(newRecommendation);

        MemberRecommendation newMemberRecommendation = MemberRecommendation.builder()
                .member(member)
                .recommendation(newRecommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        memberRecommendationRepository.save(newMemberRecommendation);

        MemberRecommendationProblem secondRec = createMemberRecommendationProblem(newMemberRecommendation, problem);

        // then: 두 번째 추천은 독립적으로 미해결 상태
        assertThat(secondRec.isSolved()).isFalse();
        assertThat(secondRec.getSolvedAt()).isNull();

        // 첫 번째 추천은 여전히 해결됨 상태 유지
        MemberRecommendationProblem firstRecReloaded = memberRecommendationProblemRepository
                .findById(firstRec.getId())
                .orElseThrow();
        assertThat(firstRecReloaded.isSolved()).isTrue();
        assertThat(firstRecReloaded.getSolvedAt()).isEqualTo(firstSolvedAt);

        // 각각 독립적으로 해결 인증 가능
        secondRec.markAsSolved();
        memberRecommendationProblemRepository.save(secondRec);

        assertThat(secondRec.isSolved()).isTrue();
        assertThat(secondRec.getSolvedAt()).isNotNull();
        assertThat(secondRec.getSolvedAt()).isAfter(firstSolvedAt);
    }

    @Test
    @DisplayName("solvedAt이 null이면 isSolved()는 false를 반환한다")
    void solvedAt_null_isSolvedReturnsFalse() {
        // given
        MemberRecommendationProblem mrp = createMemberRecommendationProblem(memberRecommendation, problem);

        // then
        assertThat(mrp.getSolvedAt()).isNull();
        assertThat(mrp.isSolved()).isFalse();
    }

    @Test
    @DisplayName("markAsSolved() 호출 시 solvedAt이 설정되고 isSolved()는 true를 반환한다")
    void markAsSolved_setsSolvedAtAndReturnsTrue() {
        // given
        MemberRecommendationProblem mrp = createMemberRecommendationProblem(memberRecommendation, problem);
        assertThat(mrp.isSolved()).isFalse();

        // when
        LocalDateTime beforeSolve = LocalDateTime.now();
        mrp.markAsSolved();
        LocalDateTime afterSolve = LocalDateTime.now();

        // then
        assertThat(mrp.getSolvedAt()).isNotNull();
        assertThat(mrp.isSolved()).isTrue();
        assertThat(mrp.getSolvedAt()).isBetween(beforeSolve, afterSolve);
    }

    @Test
    @DisplayName("팀 통계 쿼리를 위한 teamId denormalization이 올바르게 동작한다")
    void teamId_denormalization_worksForTeamStats() {
        // given: 동일한 팀에서 여러 문제 추천
        Problem problem2 = Problem.builder()
                .id(1002L)
                .title("문제 2")
                .level(11)
                .build();
        problemRepository.save(problem2);

        MemberRecommendationProblem mrp1 = createMemberRecommendationProblem(memberRecommendation, problem);
        MemberRecommendationProblem mrp2 = createMemberRecommendationProblem(memberRecommendation, problem2);

        mrp1.markAsSolved();
        memberRecommendationProblemRepository.save(mrp1);

        // when: 팀 ID로 문제 조회
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        List<MemberRecommendationProblem> teamProblems = memberRecommendationProblemRepository
                .findByTeamIdAndDateRange(team.getId(), startOfToday, endOfToday);

        // then
        assertThat(teamProblems).hasSize(2);
        assertThat(teamProblems).allMatch(mrp -> mrp.getTeamId().equals(team.getId()));

        // 해결한 문제 개수 확인
        long solvedCount = memberRecommendationProblemRepository
                .countSolvedByTeamIdAndDateRange(team.getId(), startOfToday, endOfToday);
        assertThat(solvedCount).isEqualTo(1);

        // 전체 문제 개수 확인
        long totalCount = memberRecommendationProblemRepository
                .countByTeamIdAndDateRange(team.getId(), startOfToday, endOfToday);
        assertThat(totalCount).isEqualTo(2);
    }

    @Test
    @DisplayName("teamId로 특정 기간 내 팀 통계 조회가 가능하다")
    void teamStats_queryByTeamIdAndDateRange() {
        // given: 추천 문제 생성 및 해결
        MemberRecommendationProblem mrp = createMemberRecommendationProblem(memberRecommendation, problem);
        mrp.markAsSolved();
        memberRecommendationProblemRepository.save(mrp);

        Long teamId = team.getId();
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        // when: teamId로 통계 조회
        long solvedCount = memberRecommendationProblemRepository
                .countSolvedByTeamIdAndDateRange(teamId, startOfToday, endOfToday);

        long totalCount = memberRecommendationProblemRepository
                .countByTeamIdAndDateRange(teamId, startOfToday, endOfToday);

        List<MemberRecommendationProblem> problems = memberRecommendationProblemRepository
                .findByTeamIdAndDateRange(teamId, startOfToday, endOfToday);

        // then: teamId denormalization 덕분에 JOIN 없이 빠른 통계 조회 가능
        assertThat(solvedCount).isEqualTo(1);
        assertThat(totalCount).isEqualTo(1);
        assertThat(problems).hasSize(1);
        assertThat(problems.get(0).getTeamId()).isEqualTo(teamId);
        assertThat(problems.get(0).getTeamName()).isEqualTo(team.getName());
    }

    /**
     * MemberRecommendationProblem 생성 헬퍼 메서드
     */
    private MemberRecommendationProblem createMemberRecommendationProblem(
            MemberRecommendation memberRecommendation,
            Problem problem
    ) {
        MemberRecommendationProblem mrp = MemberRecommendationProblem.builder()
                .member(member)
                .memberRecommendation(memberRecommendation)
                .problem(problem)
                .teamId(team.getId())
                .teamName(team.getName())
                .build();
        return memberRecommendationProblemRepository.save(mrp);
    }
}