package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.repository.MemberSolvedProblemRepository;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.team.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import com.ryu.studyhelper.member.dto.response.DailySolvedResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MemberSolvedProblemRepository memberSolvedProblemRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SolvedAcService solvedAcService;

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
    @DisplayName("verifyProblemSolved 메서드")
    class VerifyProblemSolvedTest {

        @Test
        @DisplayName("성공 - 문제 해결 인증")
        void success() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberSolvedProblemRepository.existsByMemberIdAndProblemId(1L, 1000L)).willReturn(false);
            given(solvedAcService.hasUserSolvedProblem("testuser", 1000L)).willReturn(true);
            given(memberSolvedProblemRepository.save(any(MemberSolvedProblem.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            memberService.verifyProblemSolved(1L, 1000L);

            // then
            verify(memberSolvedProblemRepository).save(any(MemberSolvedProblem.class));
        }

        @Test
        @DisplayName("실패 - 회원을 찾을 수 없음")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 문제를 찾을 수 없음")
        void fail_problemNotFound() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 이미 인증된 문제")
        void fail_alreadySolved() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberSolvedProblemRepository.existsByMemberIdAndProblemId(1L, 1000L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.ALREADY_SOLVED);
        }

        @Test
        @DisplayName("실패 - 핸들이 등록되지 않음")
        void fail_handleNotRegistered() {
            // given
            Member memberWithoutHandle = Member.builder()
                    .id(1L)
                    .provider("google")
                    .providerId("google_123")
                    .email("test@example.com")
                    .handle(null)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(memberWithoutHandle));

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - solved.ac에서 해결 확인 안됨")
        void fail_notSolvedYet() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(problemRepository.findById(1000L)).willReturn(Optional.of(problem));
            given(memberSolvedProblemRepository.existsByMemberIdAndProblemId(1L, 1000L)).willReturn(false);
            given(solvedAcService.hasUserSolvedProblem("testuser", 1000L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_SOLVED_YET);
        }

        @Test
        @DisplayName("실패 - 빈 핸들")
        void fail_emptyHandle() {
            // given
            Member memberWithEmptyHandle = Member.builder()
                    .id(1L)
                    .provider("google")
                    .providerId("google_123")
                    .email("test@example.com")
                    .handle("")
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(memberWithEmptyHandle));

            // when & then
            assertThatThrownBy(() -> memberService.verifyProblemSolved(1L, 1000L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("withdraw 메서드")
    class WithdrawTest {

        @Test
        @DisplayName("성공 - 팀 미소속 회원 탈퇴")
        void success() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(teamMemberRepository.existsByMemberId(1L)).willReturn(false);

            // when
            memberService.withdraw(1L);

            // then
            assertThat(member.isDeleted()).isTrue();
            assertThat(member.getEmail()).isEqualTo("WITHDRAWN_1@deleted.local");
            assertThat(member.getProviderId()).isEqualTo("WITHDRAWN_1");
            assertThat(member.getHandle()).isNull();
            assertThat(member.isVerified()).isFalse();
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("실패 - 회원을 찾을 수 없음")
        void fail_memberNotFound() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.withdraw(1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 팀에 소속된 회원")
        void fail_memberHasTeam() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(teamMemberRepository.existsByMemberId(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.withdraw(1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_HAS_TEAM);

            // 탈퇴 처리되지 않았는지 확인
            assertThat(member.isDeleted()).isFalse();
            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getDailySolved 메서드")
    class GetDailySolvedTest {

        @Mock
        private Clock clock;

        private MemberService memberServiceWithClock;

        @BeforeEach
        void setUp() {
            // 2024-11-28 14:00 (오후 2시)으로 고정
            ZonedDateTime fixedTime = ZonedDateTime.of(2024, 11, 28, 14, 0, 0, 0, ZoneId.systemDefault());
            given(clock.instant()).willReturn(fixedTime.toInstant());
            given(clock.getZone()).willReturn(ZoneId.systemDefault());

            memberServiceWithClock = new MemberService(
                    memberRepository,
                    problemRepository,
                    memberSolvedProblemRepository,
                    teamMemberRepository,
                    solvedAcService,
                    null, // jwtUtil
                    null, // mailSendService
                    clock
            );
        }

        @Test
        @DisplayName("성공 - 7일간 일별 풀이 현황 조회")
        void success_getDailySolved() {
            // given
            Problem problem1 = Problem.builder().id(1000L).title("A+B").titleKo("A+B").level(1).build();
            Problem problem2 = Problem.builder().id(7576L).title("토마토").titleKo("토마토").level(11).build();

            MemberSolvedProblem solved1 = mock(MemberSolvedProblem.class);
            given(solved1.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 10, 0)); // 11/28 10:00
            given(solved1.getProblem()).willReturn(problem1);

            MemberSolvedProblem solved2 = mock(MemberSolvedProblem.class);
            given(solved2.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 15, 0)); // 11/28 15:00
            given(solved2.getProblem()).willReturn(problem2);

            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtBetween(any(), any(), any()))
                    .willReturn(List.of(solved1, solved2));

            // when
            DailySolvedResponse response = memberServiceWithClock.getDailySolved(1L, 7);

            // then
            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.dailySolved()).hasSize(7);

            // 11/28에 2문제
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
            // given
            Problem problem1 = Problem.builder().id(1000L).title("A+B").titleKo("A+B").level(1).build();

            MemberSolvedProblem solved = mock(MemberSolvedProblem.class);
            // 11/28 05:30 → 11/27로 계산되어야 함
            given(solved.getSolvedAt()).willReturn(LocalDateTime.of(2024, 11, 28, 5, 30));
            given(solved.getProblem()).willReturn(problem1);

            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtBetween(any(), any(), any()))
                    .willReturn(List.of(solved));

            // when
            DailySolvedResponse response = memberServiceWithClock.getDailySolved(1L, 7);

            // then
            // 11/27에 1문제
            DailySolvedResponse.DailySolved nov27 = response.dailySolved().stream()
                    .filter(d -> d.date().equals("2024-11-27"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nov27.count()).isEqualTo(1);

            // 11/28에 0문제
            DailySolvedResponse.DailySolved nov28 = response.dailySolved().stream()
                    .filter(d -> d.date().equals("2024-11-28"))
                    .findFirst()
                    .orElseThrow();
            assertThat(nov28.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공 - 풀이가 없는 경우 빈 리스트")
        void success_noSolved() {
            // given
            given(memberSolvedProblemRepository.findByMemberIdAndSolvedAtBetween(any(), any(), any()))
                    .willReturn(List.of());

            // when
            DailySolvedResponse response = memberServiceWithClock.getDailySolved(1L, 7);

            // then
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

        @Test
        @DisplayName("실패 - days가 1 미만일 때 예외 발생")
        void fail_daysLessThanOne() {
            // given
            MemberService service = new MemberService(
                    memberRepository, problemRepository, memberSolvedProblemRepository,
                    teamMemberRepository, solvedAcService, null, null, Clock.systemDefaultZone()
            );

            // when & then
            assertThatThrownBy(() -> service.getDailySolved(1L, 0))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        @Test
        @DisplayName("실패 - days가 730 초과일 때 예외 발생")
        void fail_daysGreaterThan730() {
            // given
            MemberService service = new MemberService(
                    memberRepository, problemRepository, memberSolvedProblemRepository,
                    teamMemberRepository, solvedAcService, null, null, Clock.systemDefaultZone()
            );

            // when & then
            assertThatThrownBy(() -> service.getDailySolved(1L, 731))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_DAYS_RANGE);
        }
    }
}