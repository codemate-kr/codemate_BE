package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.infrastructure.mail.sender.MailSender;
import com.ryu.studyhelper.recommendation.mailbuilder.RecommendationMailBuilder;
import com.ryu.studyhelper.recommendation.service.RecommendationEmailService;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 배치 트랜잭션 문제 상황 테스트
 *
 * 문제: 클래스 레벨 @Transactional로 인해 전체 배치가 하나의 트랜잭션으로 묶임
 * 결과:
 *   1. 중간에 DB 오류 발생 시 이전 성공 건도 롤백
 *   2. 메일은 이미 발송됐는데 DB는 PENDING → 중복 발송 위험
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionIssueTest {

    @Autowired
    private Clock clock;

    @Autowired
    private RecommendationEmailService recommendationEmailService;

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

    @MockBean
    private MailSender mailSender;

    @MockBean
    private RecommendationMailBuilder recommendationMailBuilder;

    private Member member1;
    private Member member2;
    private Member member3;
    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        memberRecommendationRepository.deleteAll();
        recommendationProblemRepository.deleteAll();
        recommendationRepository.deleteAll();

        // 테스트 회원 생성
        member1 = createMember("user1", "user1@test.com");
        member2 = createMember("user2", "user2@test.com");
        member3 = createMember("user3", "user3@test.com");

        // 추천 및 문제 생성
        recommendation = createRecommendation();
    }

    @Test
    @DisplayName("정상 케이스: 모든 메일 발송 성공")
    void 모든_메일_발송_성공() {
        // given: 3명의 회원에게 메일 발송 예정
        createMemberRecommendation(member1, recommendation);
        createMemberRecommendation(member2, recommendation);
        createMemberRecommendation(member3, recommendation);

        AtomicInteger mailSendCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            mailSendCount.incrementAndGet();
            System.out.println("메일 발송 완료: " + mailSendCount.get() + "번째");
            return null;
        }).when(mailSender).send(any());

        // when
        recommendationEmailService.sendAll();

        // then
        System.out.println("\n=== 결과 ===");
        System.out.println("메일 발송 횟수: " + mailSendCount.get());

        List<MemberRecommendation> results = memberRecommendationRepository.findAll();
        long sentCount = results.stream()
                .filter(mr -> mr.getEmailSendStatus() == EmailSendStatus.SENT)
                .count();

        System.out.println("DB SENT 상태: " + sentCount);

        assertThat(mailSendCount.get()).isEqualTo(3);
        assertThat(sentCount).isEqualTo(3);
    }

    @Test
    @DisplayName("문제 상황: 3번째 메일에서 예외 발생 시 1,2번 상태 확인")
    void 중간_메일_실패시_이전_성공건_상태_확인() {
        // given: 3명의 회원에게 메일 발송 예정
        createMemberRecommendation(member1, recommendation);
        createMemberRecommendation(member2, recommendation);
        createMemberRecommendation(member3, recommendation);

        AtomicInteger mailSendCount = new AtomicInteger(0);

        // 3번째 메일에서 예외 발생
        doAnswer(invocation -> {
            int count = mailSendCount.incrementAndGet();
            System.out.println("메일 발송 시도: " + count + "번째");
            if (count == 3) {
                throw new RuntimeException("3번째 메일 발송 실패 (Gmail SMTP 오류)");
            }
            return null;
        }).when(mailSender).send(any());

        // when
        recommendationEmailService.sendAll();

        // then
        System.out.println("\n=== 결과 ===");
        System.out.println("메일 발송 시도 횟수: " + mailSendCount.get());

        List<MemberRecommendation> results = memberRecommendationRepository.findAll();
        for (MemberRecommendation mr : results) {
            System.out.println("회원 " + mr.getMember().getHandle() + ": " + mr.getEmailSendStatus());
        }

        long sentCount = results.stream()
                .filter(mr -> mr.getEmailSendStatus() == EmailSendStatus.SENT)
                .count();
        long failedCount = results.stream()
                .filter(mr -> mr.getEmailSendStatus() == EmailSendStatus.FAILED)
                .count();

        System.out.println("SENT: " + sentCount + ", FAILED: " + failedCount);

        // 현재 구현: try-catch로 개별 처리되므로 1,2는 SENT, 3은 FAILED
        assertThat(sentCount).isEqualTo(2);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("문제 시뮬레이션: 메일 발송 후 트랜잭션 롤백 시 불일치")
    void 메일_발송_후_롤백시_불일치_시뮬레이션() {
        // given
        createMemberRecommendation(member1, recommendation);
        createMemberRecommendation(member2, recommendation);

        AtomicInteger mailSendCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            mailSendCount.incrementAndGet();
            System.out.println("메일 발송 완료: " + mailSendCount.get() + "번째 (롤백되어도 취소 불가!)");
            return null;
        }).when(mailSender).send(any());

        // when & then: 시나리오 설명
        System.out.println("\n=== 문제 시나리오 ===");
        System.out.println("1. sendPendingRecommendationEmails() 호출");
        System.out.println("2. 클래스 레벨 @Transactional로 전체가 하나의 트랜잭션");
        System.out.println("3. 메일 2건 발송 성공 (Gmail SMTP)");
        System.out.println("4. save() 호출 후 메모리에만 반영 (아직 커밋 안 됨)");
        System.out.println("5. 메서드 종료 시점에 커밋 시도");
        System.out.println("6. 만약 이 시점에 DB 오류 발생하면?");
        System.out.println("   - 트랜잭션 전체 롤백");
        System.out.println("   - DB: 2건 모두 PENDING (롤백됨)");
        System.out.println("   - Gmail: 2건 이미 발송됨 (롤백 불가)");
        System.out.println("7. 다음 스케줄러 실행 시 중복 발송!");

        System.out.println("\n=== 현재 코드의 문제 ===");
        System.out.println("- RecommendationService에 클래스 레벨 @Transactional");
        System.out.println("- 배치 전체가 하나의 트랜잭션으로 묶임");
        System.out.println("- 외부 API(Gmail)는 트랜잭션 롤백 대상 아님");
    }

    @Test
    @DisplayName("문제 상황: 긴 트랜잭션으로 인한 DB 커넥션 점유")
    void 긴_트랜잭션_DB커넥션_점유_시뮬레이션() {
        // given: 10명의 회원
        for (int i = 4; i <= 10; i++) {
            Member member = createMember("user" + i, "user" + i + "@test.com");
            createMemberRecommendation(member, recommendation);
        }
        createMemberRecommendation(member1, recommendation);
        createMemberRecommendation(member2, recommendation);
        createMemberRecommendation(member3, recommendation);

        AtomicInteger mailSendCount = new AtomicInteger(0);

        doAnswer(invocation -> {
            mailSendCount.incrementAndGet();
            // 메일 발송 지연 시뮬레이션 (100ms)
            Thread.sleep(100);
            return null;
        }).when(mailSender).send(any());

        // when
        long startTime = System.currentTimeMillis();
        recommendationEmailService.sendAll();
        long endTime = System.currentTimeMillis();

        // then
        System.out.println("\n=== DB 커넥션 점유 시간 ===");
        System.out.println("메일 발송 건수: " + mailSendCount.get());
        System.out.println("총 소요 시간: " + (endTime - startTime) + "ms");
        System.out.println("이 시간 동안 DB 커넥션 1개 점유 중!");
        System.out.println("\n실제 운영 환경에서:");
        System.out.println("- 100명 x 평균 2초 = 200초 동안 커넥션 점유");
        System.out.println("- 커넥션 풀 고갈 위험");
        System.out.println("- 다른 요청 대기 발생");

        assertThat(endTime - startTime).isGreaterThan(1000);  // 최소 1초 이상
    }

    // === Helper Methods ===

    private Member createMember(String handle, String email) {
        List<Member> existing = memberRepository.findAllByHandle(handle);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return memberRepository.save(
                Member.builder()
                        .handle(handle)
                        .email(email)
                        .provider("google")
                        .providerId(handle + "_oauth_" + System.nanoTime())
                        .build()
        );
    }

    private Recommendation createRecommendation() {
        Recommendation rec = Recommendation.builder()
                .teamId(1L)
                .type(RecommendationType.SCHEDULED)
                .date(MissionCyclePolicy.getMissionDate(clock))
                .status(RecommendationStatus.SUCCESS)
                .build();
        rec = recommendationRepository.save(rec);

        // 문제 생성 및 연결
        Problem problem = problemRepository.findById(1000L)
                .orElseGet(() -> problemRepository.save(
                        Problem.builder()
                                .id(1000L)
                                .title("Test Problem")
                                .titleKo("테스트 문제")
                                .level(11)
                                .build()
                ));

        RecommendationProblem rp = RecommendationProblem.builder()
                .recommendation(rec)
                .problem(problem)
                .build();
        recommendationProblemRepository.save(rp);

        return rec;
    }

    private MemberRecommendation createMemberRecommendation(Member member, Recommendation recommendation) {
        MemberRecommendation mr = MemberRecommendation.builder()
                .member(member)
                .recommendation(recommendation)
                .teamId(1L)
                .teamName("테스트팀")
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
        return memberRecommendationRepository.save(mr);
    }
}