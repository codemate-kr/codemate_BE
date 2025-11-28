package com.ryu.studyhelper.team;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService 단위 테스트")
class TeamServiceTests {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TeamService teamService;

    private Team team;
    private Member leader;
    private Member normalMember;
    private TeamMember leaderTeamMember;
    private TeamMember normalTeamMember;

    @BeforeEach
    void setUp() {
        // 팀 생성
        team = Team.create("테스트 팀", "알고리즘 스터디");

        // 멤버 생성
        leader = Member.builder()
                .id(1L)
                .email("leader@test.com")
                .provider("google")
                .providerId("leader123")
                .role(Role.ROLE_USER)
                .isVerified(false)
                .build();

        normalMember = Member.builder()
                .id(2L)
                .email("member@test.com")
                .provider("google")
                .providerId("member123")
                .role(Role.ROLE_USER)
                .isVerified(false)
                .build();

        // 팀 멤버십 생성
        leaderTeamMember = TeamMember.createLeader(team, leader);
        normalTeamMember = TeamMember.createMember(team, normalMember);
    }

    @Test
    @DisplayName("일반 멤버는 팀을 탈퇴할 수 있다")
    void leaveTeam_normalMember_success() {
        // given
        Long teamId = 1L;
        Long memberId = normalMember.getId();

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId))
                .willReturn(Optional.of(normalTeamMember));

        // when
        teamService.leaveTeam(teamId, memberId);

        // then
        verify(teamMemberRepository).findByTeamIdAndMemberId(teamId, memberId);
        verify(teamMemberRepository).delete(normalTeamMember);
    }

    @Test
    @DisplayName("리더는 팀을 탈퇴할 수 없다")
    void leaveTeam_leader_throwsException() {
        // given
        Long teamId = 1L;
        Long leaderId = leader.getId();

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, leaderId))
                .willReturn(Optional.of(leaderTeamMember));

        // when & then
        assertThatThrownBy(() -> teamService.leaveTeam(teamId, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_LEADER_CANNOT_LEAVE);

        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 팀에서 탈퇴 시도 시 예외 발생")
    void leaveTeam_teamNotFound_throwsException() {
        // given
        Long teamId = 999L;
        Long memberId = normalMember.getId();

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.leaveTeam(teamId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_MEMBER_NOT_FOUND);

        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("팀에 속하지 않은 멤버가 탈퇴 시도 시 예외 발생")
    void leaveTeam_memberNotInTeam_throwsException() {
        // given
        Long teamId = 1L;
        Long memberId = 999L;

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.leaveTeam(teamId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_MEMBER_NOT_FOUND);

        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TeamMember의 role이 LEADER인지 올바르게 확인")
    void teamMemberRole_verification() {
        // given & when & then
        assertThat(leaderTeamMember.getRole()).isEqualTo(TeamRole.LEADER);
        assertThat(normalTeamMember.getRole()).isEqualTo(TeamRole.MEMBER);
    }

    @Test
    @DisplayName("리더는 팀을 삭제할 수 있다")
    void deleteTeam_leader_success() {
        // given
        Long teamId = 1L;
        Long leaderId = leader.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER))
                .willReturn(true);

        // when
        teamService.deleteTeam(teamId, leaderId);

        // then
        verify(teamRepository).findById(teamId);
        verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER);
        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("일반 멤버는 팀을 삭제할 수 없다")
    void deleteTeam_normalMember_throwsException() {
        // given
        Long teamId = 1L;
        Long memberId = normalMember.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> teamService.deleteTeam(teamId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 팀 삭제 시도 시 예외 발생")
    void deleteTeam_teamNotFound_throwsException() {
        // given
        Long teamId = 999L;
        Long leaderId = leader.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.deleteTeam(teamId, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

        verify(teamMemberRepository, never()).existsByTeamIdAndMemberIdAndRole(any(), any(), any());
        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 3개 미만이면 성공")
    void createTeam_lessThanLimit_success() {
        // given
        Long memberId = leader.getId();
        var request = new com.ryu.studyhelper.team.dto.CreateTeamRequest("새로운 팀", "설명");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(2);
        given(teamRepository.save(any(Team.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        var response = teamService.create(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새로운 팀");
        assertThat(response.description()).isEqualTo("설명");
        verify(teamMemberRepository).countByMemberIdAndRole(memberId, TeamRole.LEADER);
        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 이미 3개면 예외 발생")
    void createTeam_exceedsLimit_throwsException() {
        // given
        Long memberId = leader.getId();
        var request = new com.ryu.studyhelper.team.dto.CreateTeamRequest("새로운 팀", "설명");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(3);

        // when & then
        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);

        verify(teamMemberRepository).countByMemberIdAndRole(memberId, TeamRole.LEADER);
        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 정확히 3개일 때 예외 발생")
    void createTeam_exactlyThreeTeams_throwsException() {
        // given
        Long memberId = leader.getId();
        var request = new com.ryu.studyhelper.team.dto.CreateTeamRequest("네 번째 팀", "설명");

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(3);

        // when & then
        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("팀 생성 시 존재하지 않는 멤버면 예외 발생")
    void createTeam_memberNotFound_throwsException() {
        // given
        Long memberId = 999L;
        var request = new com.ryu.studyhelper.team.dto.CreateTeamRequest("새로운 팀", "설명");

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

        verify(teamMemberRepository, never()).countByMemberIdAndRole(any(), any());
        verify(teamRepository, never()).save(any());
    }
}
