package com.ryu.studyhelper.recommendation.repository;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberRecommendationRepository 테스트")
class MemberRecommendationRepositoryTest {

    @Autowired
    private Clock clock;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationProblemRepository recommendationProblemRepository;

    @Autowired
    private MemberRecommendationRepository memberRecommendationRepository;

    @Autowired
    private EntityManager entityManager;

    private static final int MEMBER_COUNT = 10;
    private static final int PROBLEM_COUNT = 3;

    @BeforeEach
    void setUp() {
        memberRecommendationRepository.deleteAll();
        recommendationProblemRepository.deleteAll();
        recommendationRepository.deleteAll();
    }

    @Test
    @DisplayName("팀원 10명이 같은 Recommendation을 공유할 때 problems가 중복 누적되지 않아야 한다")
    void problemsAreNotInflated_whenMembersShareSameRecommendation() {
        // given
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        Recommendation recommendation = createRecommendation(missionDate);
        createRecommendationProblems(recommendation, PROBLEM_COUNT);

        for (int i = 1; i <= MEMBER_COUNT; i++) {
            Member member = createMember("user" + i, "user" + i + "@test.com");
            createMemberRecommendation(member, recommendation);
        }

        // 1차 캐시 초기화 — 쿼리 결과가 캐시 없이 직접 로딩되도록
        entityManager.flush();
        entityManager.clear();

        // when
        List<MemberRecommendation> memberRecommendations = memberRecommendationRepository
                .findByRecommendationDateAndEmailSendStatus(missionDate, EmailSendStatus.PENDING);

        // then
        assertThat(memberRecommendations).hasSize(MEMBER_COUNT);

        memberRecommendations.forEach(mr -> {
            int problemCount = mr.getRecommendation().getProblems().size();
            System.out.printf("[DEBUG] 회원 %-10s → problems: %d개%n", mr.getMember().getHandle(), problemCount);
            assertThat(mr.getRecommendation().getProblems())
                    .as("회원 %s의 problems 수", mr.getMember().getHandle())
                    .hasSize(PROBLEM_COUNT);
        });
    }

    @Test
    @DisplayName("1명에 대해 같은 쿼리를 10번 반복하면 problems가 10배 누적된다")
    void problemsAreInflated_whenSameQueryRepeated() {
        // given
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        Recommendation recommendation = createRecommendation(missionDate);
        createRecommendationProblems(recommendation, PROBLEM_COUNT);
        createMember("user1", "user1@test.com");
        createMemberRecommendation(createMember("solo", "solo@test.com"), recommendation);

        entityManager.flush();
        entityManager.clear();

        // when: 같은 쿼리 10번 반복 (캐시 초기화 없이)
        for (int i = 0; i < 10; i++) {
            memberRecommendationRepository
                    .findByRecommendationDateAndEmailSendStatus(missionDate, EmailSendStatus.PENDING);
        }

        List<MemberRecommendation> results = memberRecommendationRepository
                .findByRecommendationDateAndEmailSendStatus(missionDate, EmailSendStatus.PENDING);

        // then
        results.forEach(mr -> {
            int problemCount = mr.getRecommendation().getProblems().size();
            System.out.printf("[DEBUG] 회원 %-10s → problems: %d개 (기대: %d개)%n",
                    mr.getMember().getHandle(), problemCount, PROBLEM_COUNT);
        });

        results.forEach(mr ->
                assertThat(mr.getRecommendation().getProblems())
                        .as("반복 쿼리 후 회원 %s의 problems 수", mr.getMember().getHandle())
                        .hasSize(PROBLEM_COUNT)
        );
    }

    // === Helper Methods ===

    private Recommendation createRecommendation(LocalDate date) {
        return recommendationRepository.save(
                Recommendation.builder()
                        .teamId(1L)
                        .squadId(1L)
                        .type(RecommendationType.SCHEDULED)
                        .date(date)
                        .status(RecommendationStatus.SUCCESS)
                        .build()
        );
    }

    private void createRecommendationProblems(Recommendation recommendation, int count) {
        for (int i = 1; i <= count; i++) {
            final long problemId = 9000L + i;
            Problem problem = problemRepository.findById(problemId)
                    .orElseGet(() -> problemRepository.save(
                            Problem.builder()
                                    .id(problemId)
                                    .title("Problem " + problemId)
                                    .titleKo("문제 " + problemId)
                                    .level(1)
                                    .build()
                    ));
            recommendationProblemRepository.save(
                    RecommendationProblem.builder()
                            .recommendation(recommendation)
                            .problem(problem)
                            .build()
            );
        }
    }

    private Member createMember(String handle, String email) {
        return memberRepository.save(
                Member.builder()
                        .handle(handle)
                        .email(email)
                        .provider("google")
                        .providerId(handle + "_" + System.nanoTime())
                        .build()
        );
    }

    private MemberRecommendation createMemberRecommendation(Member member, Recommendation recommendation) {
        return memberRecommendationRepository.save(
                MemberRecommendation.builder()
                        .member(member)
                        .recommendation(recommendation)
                        .teamId(1L)
                        .squadId(1L)
                        .teamName("테스트팀")
                        .emailSendStatus(EmailSendStatus.PENDING)
                        .build()
        );
    }
}
