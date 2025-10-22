package com.ryu.studyhelper.team;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.team.domain.*;
import com.ryu.studyhelper.team.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService 단위 테스트")
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TeamService teamService;

    private Member testMember;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .handle("testHandle")
                .isVerified(true)
                .role(Role.ROLE_USER)
                .provider("google")
                .providerId("12345")
                .build();

        testTeam = Team.builder()
                .id(1L)
                .name("Test Team")
                .description("Test Description")
                .recommendationStatus(RecommendationStatus.INACTIVE)
                .recommendationDays(RecommendationDayOfWeek.INACTIVE)
                .problemDifficultyPreset(ProblemDifficultyPreset.NORMAL)
                .teamMembers(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("create() - 팀 생성")
    class CreateTests {

        @Test
        @DisplayName("성공: 팀을 생성하고 생성자를 리더로 추가한다")
        void create_Success() {
            // given
            CreateTeamRequest request = new CreateTeamRequest("New Team", "New Description");
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(teamRepository.save(any(Team.class))).willReturn(testTeam);
            given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateTeamResponse response = teamService.create(request, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Test Team");
            assertThat(response.description()).isEqualTo("Test Description");
            verify(memberRepository).findById(1L);
            verify(teamRepository).save(any(Team.class));
            verify(teamMemberRepository).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void create_MemberNotFound() {
            // given
            CreateTeamRequest request = new CreateTeamRequest("New Team", "Description");
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.create(request, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findById(999L);
            verify(teamRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("joinTeam() - 팀 가입")
    class JoinTeamTests {

        @Test
        @DisplayName("성공: 팀에 가입한다")
        void joinTeam_Success() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(teamMemberRepository.existsByTeamIdAndMemberId(1L, 1L)).willReturn(false);
            given(teamMemberRepository.save(any(TeamMember.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            teamService.joinTeam(1L, 1L);

            // then
            ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
            verify(teamMemberRepository).save(captor.capture());
            TeamMember savedTeamMember = captor.getValue();
            assertThat(savedTeamMember.getTeam()).isEqualTo(testTeam);
            assertThat(savedTeamMember.getMember()).isEqualTo(testMember);
            assertThat(savedTeamMember.getRole()).isEqualTo(TeamRole.MEMBER);
        }

        @Test
        @DisplayName("실패: 이미 팀에 속해있을 때 (변경된 예외 타입)")
        void joinTeam_AlreadyMember() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(teamMemberRepository.existsByTeamIdAndMemberId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> teamService.joinTeam(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.ALREADY_MAP_EXIST);

            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void joinTeam_TeamNotFound() {
            // given
            given(teamRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.joinTeam(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void joinTeam_MemberNotFound() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.joinTeam(1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(teamMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getRecommendationSettings() - 팀 추천 설정 조회")
    class GetRecommendationSettingsTests {

        @Test
        @DisplayName("성공: 팀 추천 설정을 조회한다")
        void getRecommendationSettings_Success() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));

            // when
            TeamRecommendationSettingsResponse response = teamService.getRecommendationSettings(1L);

            // then
            assertThat(response).isNotNull();
            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void getRecommendationSettings_TeamNotFound() {
            // given
            given(teamRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.getRecommendationSettings(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

            verify(teamRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("updateRecommendationSettings() - 팀 추천 설정 업데이트")
    class UpdateRecommendationSettingsTests {

        @Test
        @DisplayName("성공: 팀 추천 설정을 업데이트한다 (프리셋 모드)")
        void updateRecommendationSettings_Success_PresetMode() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY, RecommendationDayOfWeek.FRIDAY),
                    ProblemDifficultyPreset.HARD,
                    null,
                    null
            );
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when
            TeamRecommendationSettingsResponse response = teamService.updateRecommendationSettings(1L, request, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.problemDifficultyPreset()).isEqualTo(ProblemDifficultyPreset.HARD);
            verify(teamRepository).findById(1L);
            verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER);
        }

        @Test
        @DisplayName("성공: 팀 추천 설정을 업데이트한다 (커스텀 모드)")
        void updateRecommendationSettings_Success_CustomMode() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.TUESDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    5,
                    15
            );
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when
            TeamRecommendationSettingsResponse response = teamService.updateRecommendationSettings(1L, request, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.problemDifficultyPreset()).isEqualTo(ProblemDifficultyPreset.CUSTOM);
            assertThat(response.customMinLevel()).isEqualTo(5);
            assertThat(response.customMaxLevel()).isEqualTo(15);
            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 최소 난이도가 최대 난이도보다 큼")
        void updateRecommendationSettings_InvalidLevelRange() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    20,
                    10
            );
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> teamService.updateRecommendationSettings(1L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE);

            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 팀장이 아닌 사용자")
        void updateRecommendationSettings_NotTeamLeader() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.NORMAL,
                    null,
                    null
            );
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 2L, TeamRole.LEADER))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> teamService.updateRecommendationSettings(1L, request, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

            verify(teamRepository).findById(1L);
            verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(1L, 2L, TeamRole.LEADER);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void updateRecommendationSettings_TeamNotFound() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.NORMAL,
                    null,
                    null
            );
            given(teamRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.updateRecommendationSettings(999L, request, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

            verify(teamRepository).findById(999L);
        }

        @Test
        @DisplayName("경계값: 최소/최대 난이도가 같을 때")
        void updateRecommendationSettings_EqualMinMaxLevel() {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    10,
                    10
            );
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when
            TeamRecommendationSettingsResponse response = teamService.updateRecommendationSettings(1L, request, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.customMinLevel()).isEqualTo(10);
            assertThat(response.customMaxLevel()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("disableRecommendation() - 팀 추천 비활성화")
    class DisableRecommendationTests {

        @Test
        @DisplayName("성공: 팀 추천을 비활성화한다")
        void disableRecommendation_Success() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when
            TeamRecommendationSettingsResponse response = teamService.disableRecommendation(1L, 1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.recommendationDays()).isEmpty();
            verify(teamRepository).findById(1L);
            verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER);
        }

        @Test
        @DisplayName("실패: 팀장이 아닌 사용자")
        void disableRecommendation_NotTeamLeader() {
            // given
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 2L, TeamRole.LEADER))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> teamService.disableRecommendation(1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);

            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void disableRecommendation_TeamNotFound() {
            // given
            given(teamRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.disableRecommendation(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

            verify(teamRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getMyTeams() - 내가 속한 팀 목록 조회")
    class GetMyTeamsTests {

        @Test
        @DisplayName("성공: 내가 속한 팀 목록을 조회한다")
        void getMyTeams_Success() {
            // given
            TeamMember teamMember1 = TeamMember.builder()
                    .id(1L)
                    .team(testTeam)
                    .member(testMember)
                    .role(TeamRole.LEADER)
                    .build();
            
            Team team2 = Team.builder()
                    .id(2L)
                    .name("Team 2")
                    .description("Description 2")
                    .build();
            
            TeamMember teamMember2 = TeamMember.builder()
                    .id(2L)
                    .team(team2)
                    .member(testMember)
                    .role(TeamRole.MEMBER)
                    .build();

            given(teamMemberRepository.findByMemberId(1L))
                    .willReturn(Arrays.asList(teamMember1, teamMember2));

            // when
            List<MyTeamResponse> responses = teamService.getMyTeams(1L);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).teamId()).isEqualTo(1L);
            assertThat(responses.get(1).teamId()).isEqualTo(2L);
            verify(teamMemberRepository).findByMemberId(1L);
        }

        @Test
        @DisplayName("성공: 소속된 팀이 없을 때 빈 리스트 반환")
        void getMyTeams_EmptyList() {
            // given
            given(teamMemberRepository.findByMemberId(1L))
                    .willReturn(Collections.emptyList());

            // when
            List<MyTeamResponse> responses = teamService.getMyTeams(1L);

            // then
            assertThat(responses).isEmpty();
            verify(teamMemberRepository).findByMemberId(1L);
        }
    }

    @Nested
    @DisplayName("getTeamMembers() - 팀 멤버 목록 조회")
    class GetTeamMembersTests {

        @Test
        @DisplayName("성공: 팀 멤버 목록을 조회한다")
        void getTeamMembers_Success() {
            // given
            Member member2 = Member.builder()
                    .id(2L)
                    .email("member2@example.com")
                    .handle("handle2")
                    .build();

            TeamMember teamMember1 = TeamMember.builder()
                    .id(1L)
                    .team(testTeam)
                    .member(testMember)
                    .role(TeamRole.LEADER)
                    .build();

            TeamMember teamMember2 = TeamMember.builder()
                    .id(2L)
                    .team(testTeam)
                    .member(member2)
                    .role(TeamRole.MEMBER)
                    .build();

            testTeam.getTeamMembers().addAll(Arrays.asList(teamMember1, teamMember2));
            given(teamRepository.findById(1L)).willReturn(Optional.of(testTeam));

            // when
            List<TeamMemberResponse> responses = teamService.getTeamMembers(1L, 1L);

            // then
            assertThat(responses).hasSize(2);
            verify(teamRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void getTeamMembers_TeamNotFound() {
            // given
            given(teamRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamService.getTeamMembers(999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);

            verify(teamRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("isTeamLeader() - 팀장 여부 확인")
    class IsTeamLeaderTests {

        @Test
        @DisplayName("성공: 팀장일 때 true 반환")
        void isTeamLeader_True() {
            // given
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER))
                    .willReturn(true);

            // when
            boolean result = teamService.isTeamLeader(1L, 1L);

            // then
            assertThat(result).isTrue();
            verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(1L, 1L, TeamRole.LEADER);
        }

        @Test
        @DisplayName("성공: 팀장이 아닐 때 false 반환")
        void isTeamLeader_False() {
            // given
            given(teamMemberRepository.existsByTeamIdAndMemberIdAndRole(1L, 2L, TeamRole.LEADER))
                    .willReturn(false);

            // when
            boolean result = teamService.isTeamLeader(1L, 2L);

            // then
            assertThat(result).isFalse();
            verify(teamMemberRepository).existsByTeamIdAndMemberIdAndRole(1L, 2L, TeamRole.LEADER);
        }
    }
}