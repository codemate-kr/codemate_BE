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
import com.ryu.studyhelper.team.domain.TeamMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
            given(teamMemberRepository.findByMemberId(1L)).willReturn(Collections.emptyList());

            // when
            memberService.withdraw(1L);

            // then
            assertThat(member.isDeleted()).isTrue();
            assertThat(member.getEmail()).isEqualTo("WITHDRAWN_1@deleted.local");
            assertThat(member.getProviderId()).isEqualTo("WITHDRAWN_1");
            assertThat(member.getHandle()).isNull();
            assertThat(member.isVerified()).isFalse();
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
            TeamMember teamMember = mock(TeamMember.class);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(teamMemberRepository.findByMemberId(1L)).willReturn(List.of(teamMember));

            // when & then
            assertThatThrownBy(() -> memberService.withdraw(1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_HAS_TEAM);

            // 탈퇴 처리되지 않았는지 확인
            assertThat(member.isDeleted()).isFalse();
        }
    }
}