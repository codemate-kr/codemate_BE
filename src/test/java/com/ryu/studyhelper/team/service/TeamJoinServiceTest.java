package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.notification.service.NotificationService;
import com.ryu.studyhelper.team.domain.*;
import com.ryu.studyhelper.team.dto.request.TeamJoinInviteRequest;
import com.ryu.studyhelper.team.dto.response.TeamJoinResponse;
import com.ryu.studyhelper.team.repository.TeamJoinRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamJoinService 단위 테스트")
class TeamJoinServiceTest {

    @Mock
    private TeamJoinRepository teamJoinRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MailSendService mailSendService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeamJoinService teamJoinService;

    private Team team;
    private Member leader;
    private Member targetMember;

    @BeforeEach
    void setUp() {
        team = Team.builder().id(1L).name("테스트팀").build();
        ReflectionTestUtils.setField(team, "id", 1L);

        leader = Member.builder().id(1L).handle("leader").email("leader@test.com").build();
        ReflectionTestUtils.setField(leader, "id", 1L);

        targetMember = Member.builder().id(2L).handle("target").email("target@test.com").build();
        ReflectionTestUtils.setField(targetMember, "id", 2L);
    }

    @Nested
    @DisplayName("inviteMember")
    class InviteMember {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            TeamJoinInviteRequest request = new TeamJoinInviteRequest(1L, 2L);
            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER)).willReturn(true);
            given(memberRepository.findById(1L)).willReturn(Optional.of(leader));
            given(memberRepository.findById(2L)).willReturn(Optional.of(targetMember));
            given(teamMemberRepository.existsByTeamIdAndMemberId(1L, 2L)).willReturn(false);
            given(teamJoinRepository.existsByTeamIdAndTargetMemberIdAndTypeAndStatus(1L, 2L, TeamJoinType.INVITATION, TeamJoinStatus.PENDING)).willReturn(false);
            given(teamJoinRepository.save(any(TeamJoin.class))).willAnswer(invocation -> {
                TeamJoin tj = invocation.getArgument(0);
                ReflectionTestUtils.setField(tj, "id", 1L);
                return tj;
            });

            // when
            TeamJoinResponse response = teamJoinService.inviteMember(request, 1L);

            // then
            assertThat(response.teamId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(TeamJoinStatus.PENDING);
            verify(mailSendService).sendTeamInvitationEmail(any(TeamJoin.class));
        }

        @Test
        @DisplayName("실패 - 팀장 권한 없음")
        void fail_notLeader() {
            TeamJoinInviteRequest request = new TeamJoinInviteRequest(1L, 2L);
            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER)).willReturn(false);

            assertThatThrownBy(() -> teamJoinService.inviteMember(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("실패 - 자기 자신 초대")
        void fail_inviteSelf() {
            TeamJoinInviteRequest request = new TeamJoinInviteRequest(1L, 1L);
            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER)).willReturn(true);
            given(memberRepository.findById(1L)).willReturn(Optional.of(leader));

            assertThatThrownBy(() -> teamJoinService.inviteMember(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.CANNOT_INVITE_SELF);
        }

        @Test
        @DisplayName("실패 - 이미 팀 멤버")
        void fail_alreadyMember() {
            TeamJoinInviteRequest request = new TeamJoinInviteRequest(1L, 2L);
            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER)).willReturn(true);
            given(memberRepository.findById(1L)).willReturn(Optional.of(leader));
            given(memberRepository.findById(2L)).willReturn(Optional.of(targetMember));
            given(teamMemberRepository.existsByTeamIdAndMemberId(1L, 2L)).willReturn(true);

            assertThatThrownBy(() -> teamJoinService.inviteMember(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_JOIN_ALREADY_MEMBER);
        }

        @Test
        @DisplayName("실패 - 이미 대기 중인 초대 존재")
        void fail_alreadyPending() {
            TeamJoinInviteRequest request = new TeamJoinInviteRequest(1L, 2L);
            given(teamRepository.findById(1L)).willReturn(Optional.of(team));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER)).willReturn(true);
            given(memberRepository.findById(1L)).willReturn(Optional.of(leader));
            given(memberRepository.findById(2L)).willReturn(Optional.of(targetMember));
            given(teamMemberRepository.existsByTeamIdAndMemberId(1L, 2L)).willReturn(false);
            given(teamJoinRepository.existsByTeamIdAndTargetMemberIdAndTypeAndStatus(1L, 2L, TeamJoinType.INVITATION, TeamJoinStatus.PENDING)).willReturn(true);

            assertThatThrownBy(() -> teamJoinService.inviteMember(request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_JOIN_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("accept")
    class Accept {

        @Test
        @DisplayName("성공")
        void success() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            TeamJoinResponse response = teamJoinService.accept(1L, 2L);

            assertThat(response.status()).isEqualTo(TeamJoinStatus.ACCEPTED);
            verify(teamMemberRepository).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("실패 - 권한 없음")
        void fail_noPermission() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            assertThatThrownBy(() -> teamJoinService.accept(1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_JOIN_NO_PERMISSION);
        }

        @Test
        @DisplayName("실패 - 만료됨")
        void fail_expired() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            assertThatThrownBy(() -> teamJoinService.accept(1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_JOIN_EXPIRED);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("성공")
        void success() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            TeamJoinResponse response = teamJoinService.reject(1L, 2L);

            assertThat(response.status()).isEqualTo(TeamJoinStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("성공")
        void success() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            TeamJoinResponse response = teamJoinService.cancel(1L, 1L);

            assertThat(response.status()).isEqualTo(TeamJoinStatus.CANCELED);
        }

        @Test
        @DisplayName("실패 - 대상자가 취소 시도")
        void fail_targetCannotCancel() {
            TeamJoin teamJoin = TeamJoin.builder()
                    .team(team)
                    .requester(leader)
                    .targetMember(targetMember)
                    .type(TeamJoinType.INVITATION)
                    .status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(teamJoin, "id", 1L);

            given(teamJoinRepository.findById(1L)).willReturn(Optional.of(teamJoin));

            assertThatThrownBy(() -> teamJoinService.cancel(1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_JOIN_NO_PERMISSION);
        }
    }

    @Nested
    @DisplayName("getReceivedList")
    class GetReceivedList {

        @Test
        @DisplayName("만료된 초대는 제외")
        void excludeExpired() {
            TeamJoin valid = TeamJoin.builder()
                    .team(team).requester(leader).targetMember(targetMember)
                    .type(TeamJoinType.INVITATION).status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            ReflectionTestUtils.setField(valid, "id", 1L);

            TeamJoin expired = TeamJoin.builder()
                    .team(team).requester(leader).targetMember(targetMember)
                    .type(TeamJoinType.INVITATION).status(TeamJoinStatus.PENDING)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();
            ReflectionTestUtils.setField(expired, "id", 2L);

            given(teamJoinRepository.findByTargetMemberIdAndStatusAndExpiresAtAfter(
                    eq(2L), eq(TeamJoinStatus.PENDING), any(LocalDateTime.class)))
                    .willReturn(List.of(valid));

            List<TeamJoinResponse> result = teamJoinService.getReceivedList(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }
    }
}