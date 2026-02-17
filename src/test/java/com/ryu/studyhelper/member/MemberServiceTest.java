package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.solve.service.SolveService;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SolveService solveService;

    private Member member;

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
}
