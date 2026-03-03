package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.solve.domain.MemberSolvedProblem;
import com.ryu.studyhelper.solve.dto.response.DailySolvedResponse;
import com.ryu.studyhelper.solve.repository.MemberSolvedProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolveService 단위 테스트")
class SolveServiceTest {

    @InjectMocks
    private SolveService solveService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @Mock
    private MemberSolvedProblemRepository memberSolvedProblemRepository;

    @Mock
    private Clock clock;

    private Member member;
    private Problem problem;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .provider("google")
                .providerId("google_123")
                .email("test@example.com")
                .handle("testuser")
                .isVerified(true)
                .build();

        problem = Problem.builder()
                .id(1000L)
                .title("A+B")
                .titleKo("A+B")
                .level(1)
                .build();
    }

    @Nested
    @DisplayName("validateAndGetHandle 메서드")
    class ValidateAndGetHandleTest {

        @Test
        @DisplayName("성공 - 핸들 반환")
        void success() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberRecommendationRepository.existsByMemberIdAndRecommendedProblemId(1L, 1000L)).willReturn(true);
            given(memberSolvedProblemRepository.existsByMemberIdAndProblemId(1L, 1000L)).willReturn(false);

            String handle = solveService.validateAndGetHandle(1L, 1000L);

            assertThat(handle).isEqualTo("testuser");
        }

        @Test
        @DisplayName("실패 - 회원을 찾을 수 없음")
        void fail_memberNotFound() {
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 문제를 찾을 수 없음")
        void fail_problemNotFound() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 이미 인증된 문제")
        void fail_alreadySolved() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberRecommendationRepository.existsByMemberIdAndRecommendedProblemId(1L, 1000L)).willReturn(true);
            given(memberSolvedProblemRepository.existsByMemberIdAndProblemId(1L, 1000L)).willReturn(true);

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.ALREADY_SOLVED);
        }

        @Test
        @DisplayName("실패 - 핸들이 등록되지 않음")
        void fail_handleNotRegistered() {
            Member memberWithoutHandle = Member.builder()
                    .id(1L)
                    .provider("google")
                    .providerId("google_123")
                    .email("test@example.com")
                    .handle(null)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(memberWithoutHandle));

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 빈 핸들")
        void fail_emptyHandle() {
            Member memberWithEmptyHandle = Member.builder()
                    .id(1L)
                    .provider("google")
                    .providerId("google_123")
                    .email("test@example.com")
                    .handle("")
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(memberWithEmptyHandle));

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 추천 목록에 없는 문제")
        void fail_problemNotInRecommendation() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberRecommendationRepository.existsByMemberIdAndRecommendedProblemId(1L, 1000L)).willReturn(false);

            assertThatThrownBy(() -> solveService.validateAndGetHandle(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_IN_RECOMMENDATION);
        }
    }

    @Nested
    @DisplayName("recordSolved 메서드")
    class RecordSolvedTest {

        @Test
        @DisplayName("성공 - 풀이 저장")
        void success() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberSolvedProblemRepository.saveAndFlush(any(MemberSolvedProblem.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            solveService.recordSolved(1L, 1000L);

            verify(memberSolvedProblemRepository).saveAndFlush(any(MemberSolvedProblem.class));
        }

        @Test
        @DisplayName("실패 - 회원을 찾을 수 없음")
        void fail_memberNotFound() {
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solveService.recordSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 문제를 찾을 수 없음")
        void fail_problemNotFound() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solveService.recordSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 동시 요청으로 인한 중복 저장")
        void fail_concurrentDuplicate() {
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberSolvedProblemRepository.saveAndFlush(any(MemberSolvedProblem.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> solveService.recordSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.ALREADY_SOLVED);
        }
    }

    @Nested
    @DisplayName("getDailySolved 메서드")
    class GetDailySolvedTest {

        @Mock
        private Clock fixedClock;

        private SolveService solveServiceWithClock;

        @BeforeEach
        void setUp() {
            ZonedDateTime fixedTime = ZonedDateTime.of(2024, 11, 28, 14, 0, 0, 0, ZoneId.of("Asia/Seoul"));
            given(fixedClock.instant()).willReturn(fixedTime.toInstant());
            given(fixedClock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));

            solveServiceWithClock = new SolveService(
                    memberRepository,
                    problemRepository,
                    memberRecommendationRepository,
                    memberSolvedProblemRepository,
                    fixedClock
            );
        }

        @Test
        @DisplayName("성공 - 7일간 일별 풀이 현황 조회")
        void success_getDailySolved() {
            Problem problem1 = Problem.builder().id(1000L).title("A+B").titleKo("A+B").level(1).build();
            Problem problem2 = Problem.builder().id(7576L).title("토마토").titleKo("토마토").level(11).build();

            MemberSolvedProblem solved1 = mock(MemberSolvedProblem.class);
            given(solved1.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 10, 0));
            given(solved1.getProblem()).willReturn(problem1);

            MemberSolvedProblem solved2 = mock(MemberSolvedProblem.class);
            given(solved2.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 15, 0));
            given(solved2.getProblem()).willReturn(problem2);

            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(any(), any(), any()))
                    .willReturn(List.of(solved1, solved2));

            DailySolvedResponse response = solveServiceWithClock.getDailySolved(1L, 7);

            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.dailySolved()).hasSize(7);

            DailySolvedResponse.DailySolved nov28 = response.dailySolved().stream()
                    .filter(d -> d.date().equals("2024-11-28"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nov28.count()).isEqualTo(2);
            assertThat(nov28.problems()).hasSize(2);
        }

        @Test
        @DisplayName("성공 - 오전 6시 이전은 전날로 계산")
        void success_before6am_countAsPreviousDay() {
            Problem problem1 = Problem.builder().id(1000L).title("A+B").titleKo("A+B").level(1).build();

            MemberSolvedProblem solved = mock(MemberSolvedProblem.class);
            given(solved.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 5, 30));
            given(solved.getProblem()).willReturn(problem1);

            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(any(), any(), any()))
                    .willReturn(List.of(solved));

            DailySolvedResponse response = solveServiceWithClock.getDailySolved(1L, 7);

            DailySolvedResponse.DailySolved nov27 = response.dailySolved().stream()
                    .filter(d -> d.date().equals("2024-11-27"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nov27.count()).isEqualTo(1);

            DailySolvedResponse.DailySolved nov28 = response.dailySolved().stream()
                    .filter(d -> d.date().equals("2024-11-28"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nov28.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공 - 풀이가 없는 경우 빈 리스트")
        void success_noSolved() {
            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            DailySolvedResponse response = solveServiceWithClock.getDailySolved(1L, 7);

            assertThat(response.totalCount()).isEqualTo(0);
            assertThat(response.dailySolved()).hasSize(7);
            response.dailySolved().forEach(daily -> {
                assertThat(daily.count()).isEqualTo(0);
                assertThat(daily.problems()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("getDailySolved 메서드 - 범위 검증")
    class GetDailySolvedRangeValidationTest {

        private SolveService service;

        @BeforeEach
        void setUp() {
            service = new SolveService(
                    memberRepository,
                    problemRepository,
                    memberRecommendationRepository,
                    memberSolvedProblemRepository,
                    Clock.system(ZoneId.of("Asia/Seoul"))
            );
        }

        @Test
        @DisplayName("실패 - days가 1 미만일 때 예외 발생")
        void fail_daysLessThanOne() {
            assertThatThrownBy(() -> service.getDailySolved(1L, 0))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        @Test
        @DisplayName("실패 - days가 730 초과일 때 예외 발생")
        void fail_daysGreaterThan730() {
            assertThatThrownBy(() -> service.getDailySolved(1L, 731))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        @Test
        @DisplayName("성공 - 최소 경계값 days=1")
        void success_minBoundary() {
            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            DailySolvedResponse response = service.getDailySolved(1L, 1);

            assertThat(response.dailySolved()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 최대 경계값 days=730")
        void success_maxBoundary() {
            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            DailySolvedResponse response = service.getDailySolved(1L, 730);

            assertThat(response.dailySolved()).hasSize(730);
        }
    }
}
