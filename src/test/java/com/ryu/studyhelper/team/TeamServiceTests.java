package com.ryu.studyhelper.team;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.notification.service.NotificationService;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.service.TeamService;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.dto.request.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamInfoRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamVisibilityRequest;
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

    @Mock
    private SquadRepository squadRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeamService teamService;

    private Team team;
    private Member leader;
    private Member normalMember;

    @BeforeEach
    void setUp() {
        team = Team.create("테스트 팀", "알고리즘 스터디", false);

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
    }

    @Test
    @DisplayName("리더는 팀을 삭제할 수 있다")
    void deleteTeam_leader_success() {
        Long teamId = 1L;
        Long leaderId = leader.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER))
                .willReturn(true);

        teamService.deleteTeam(teamId, leaderId);

        verify(teamRepository).findById(teamId);
        verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER);
        verify(teamRepository).delete(team);
    }

    @Test
    @DisplayName("일반 멤버는 팀을 삭제할 수 없다")
    void deleteTeam_normalMember_throwsException() {
        Long teamId = 1L;
        Long memberId = normalMember.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER))
                .willReturn(false);

        assertThatThrownBy(() -> teamService.deleteTeam(teamId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("존재하지 않는 팀 삭제 시도 시 예외 발생")
    void deleteTeam_teamNotFound_throwsException() {
        Long teamId = 999L;
        Long leaderId = leader.getId();

        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.deleteTeam(teamId, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

        verify(teamMemberRepository, never()).existsByTeamIdAndMemberIdAndRole(any(), any(), any());
        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 3개 미만이면 성공")
    void createTeam_lessThanLimit_success() {
        Long memberId = leader.getId();
        var request = new CreateTeamRequest("새로운 팀", "설명", false);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(2);
        given(teamRepository.save(any(Team.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(squadRepository.save(any(Squad.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> invocation.getArgument(0));

        var response = teamService.create(request, memberId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새로운 팀");
        assertThat(response.description()).isEqualTo("설명");
        verify(teamRepository).save(any(Team.class));
        verify(squadRepository).save(any(Squad.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 이미 3개면 예외 발생")
    void createTeam_exceedsLimit_throwsException() {
        Long memberId = leader.getId();
        var request = new CreateTeamRequest("새로운 팀", "설명", false);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(3);

        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);

        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀 생성 시 LEADER 역할이 정확히 3개일 때 예외 발생")
    void createTeam_exactlyThreeTeams_throwsException() {
        Long memberId = leader.getId();
        var request = new CreateTeamRequest("네 번째 팀", "설명", false);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(leader));
        given(teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER)).willReturn(3);

        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("팀 생성 시 존재하지 않는 멤버면 예외 발생")
    void createTeam_memberNotFound_throwsException() {
        Long memberId = 999L;
        var request = new CreateTeamRequest("새로운 팀", "설명", false);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.create(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀장은 팀 공개/비공개 설정을 변경할 수 있다")
    void updateVisibility_leader_success() {
        Long teamId = 1L;
        Long leaderId = leader.getId();
        var request = new UpdateTeamVisibilityRequest(true);

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER))
                .willReturn(true);

        teamService.updateVisibility(teamId, request, leaderId);

        assertThat(team.getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("일반 멤버는 팀 공개/비공개 설정을 변경할 수 없다")
    void updateVisibility_normalMember_throwsException() {
        Long teamId = 1L;
        Long memberId = normalMember.getId();
        var request = new UpdateTeamVisibilityRequest(true);

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER))
                .willReturn(false);

        assertThatThrownBy(() -> teamService.updateVisibility(teamId, request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

        assertThat(team.getIsPrivate()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 팀의 공개/비공개 설정 변경 시도 시 예외 발생")
    void updateVisibility_teamNotFound_throwsException() {
        Long teamId = 999L;
        Long leaderId = leader.getId();
        var request = new UpdateTeamVisibilityRequest(true);

        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.updateVisibility(teamId, request, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

        verify(teamMemberRepository, never()).existsByTeamIdAndMemberIdAndRole(any(), any(), any());
    }

    @Test
    @DisplayName("팀장은 팀 정보를 수정할 수 있다")
    void updateTeamInfo_leader_success() {
        Long teamId = 1L;
        Long leaderId = leader.getId();
        var request = new UpdateTeamInfoRequest("수정된 팀 이름", "수정된 설명", true);

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER))
                .willReturn(true);

        teamService.updateTeamInfo(teamId, request, leaderId);

        assertThat(team.getName()).isEqualTo("수정된 팀 이름");
        assertThat(team.getDescription()).isEqualTo("수정된 설명");
        assertThat(team.getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("isPrivate가 null이면 기존 공개/비공개 설정이 유지된다")
    void updateTeamInfo_nullIsPrivate_keepsOriginalValue() {
        Long teamId = 1L;
        Long leaderId = leader.getId();
        var request = new UpdateTeamInfoRequest("수정된 팀 이름", "수정된 설명", null);

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER))
                .willReturn(true);

        assertThat(team.getIsPrivate()).isFalse();

        teamService.updateTeamInfo(teamId, request, leaderId);

        assertThat(team.getName()).isEqualTo("수정된 팀 이름");
        assertThat(team.getIsPrivate()).isFalse();
    }

    @Test
    @DisplayName("일반 멤버는 팀 정보를 수정할 수 없다")
    void updateTeamInfo_normalMember_throwsException() {
        Long teamId = 1L;
        Long memberId = normalMember.getId();
        var request = new UpdateTeamInfoRequest("수정된 팀 이름", "수정된 설명", null);

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER))
                .willReturn(false);

        assertThatThrownBy(() -> teamService.updateTeamInfo(teamId, request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

        assertThat(team.getName()).isEqualTo("테스트 팀");
    }

    @Test
    @DisplayName("존재하지 않는 팀의 정보 수정 시도 시 예외 발생")
    void updateTeamInfo_teamNotFound_throwsException() {
        Long teamId = 999L;
        Long leaderId = leader.getId();
        var request = new UpdateTeamInfoRequest("수정된 팀 이름", "수정된 설명", null);

        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.updateTeamInfo(teamId, request, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

        verify(teamMemberRepository, never()).existsByTeamIdAndMemberIdAndRole(any(), any(), any());
    }

    @Test
    @DisplayName("일반 멤버는 팀을 탈퇴할 수 있다")
    void leaveTeam_normalMember_success() {
        Long teamId = 1L;
        Long memberId = normalMember.getId();
        TeamMember normalTeamMember = TeamMember.createMember(team, normalMember);

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId))
                .willReturn(Optional.of(normalTeamMember));

        teamService.leaveTeam(teamId, memberId);

        verify(teamMemberRepository).delete(normalTeamMember);
    }

    @Test
    @DisplayName("리더는 팀을 탈퇴할 수 없다")
    void leaveTeam_leader_throwsException() {
        Long teamId = 1L;
        Long leaderId = leader.getId();
        TeamMember leaderTeamMember = TeamMember.createLeader(team, leader);

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, leaderId))
                .willReturn(Optional.of(leaderTeamMember));

        assertThatThrownBy(() -> teamService.leaveTeam(teamId, leaderId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_LEADER_CANNOT_LEAVE);

        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("팀에 속하지 않은 멤버가 탈퇴 시도 시 예외 발생")
    void leaveTeam_memberNotInTeam_throwsException() {
        Long teamId = 1L;
        Long memberId = 999L;

        given(teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.leaveTeam(teamId, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_MEMBER_NOT_FOUND);

        verify(teamMemberRepository, never()).delete(any());
    }
}
