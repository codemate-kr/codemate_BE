package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.solve.service.SolveService;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.dto.response.TeamLeaderboardResponse;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamActivityService 단위 테스트")
class TeamActivityServiceTest {

    @Mock
    private TeamService teamService;

    @Mock
    private SolveService solveService;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private SquadRepository squadRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @InjectMocks
    private TeamActivityService teamActivityService;

    private static final Long TEAM_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    private TeamMember teamMember1;
    private TeamMember teamMember2;

    @BeforeEach
    void setUp() {
        Member member1 = Member.builder()
                .id(1L)
                .email("member1@test.com")
                .handle("handle1")
                .provider("google")
                .providerId("provider1")
                .role(Role.ROLE_USER)
                .isVerified(true)
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .email("member2@test.com")
                .handle("handle2")
                .provider("google")
                .providerId("provider2")
                .role(Role.ROLE_USER)
                .isVerified(true)
                .build();

        Team team = Team.create("테스트팀", "설명", false);
        Squad squad = Squad.createDefault(team);

        teamMember1 = TeamMember.createLeader(team, member1, squad.getId());
        teamMember2 = TeamMember.create(team, member2, TeamRole.MEMBER, squad.getId());
    }

    @Nested
    @DisplayName("팀 접근 권한 검증")
    class TeamAccessValidation {

        @Test
        @DisplayName("비공개 팀은 비로그인 사용자가 접근할 수 없다")
        void privateTeam_deniesAnonymousAccess() {
            // given
            willThrow(new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED))
                    .given(teamService).validateTeamAccess(TEAM_ID, null);

            // when & then
            assertThatThrownBy(() -> teamActivityService.getTeamLeaderboard(TEAM_ID, null, 30))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("존재하지 않는 팀 조회 시 예외 발생")
        void nonExistentTeam_throwsException() {
            // given
            willThrow(new CustomException(CustomResponseStatus.TEAM_NOT_FOUND))
                    .given(teamService).validateTeamAccess(TEAM_ID, MEMBER_ID);

            // when & then
            assertThatThrownBy(() -> teamActivityService.getTeamLeaderboard(TEAM_ID, MEMBER_ID, 30))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리더보드 조회")
    class LeaderboardQuery {

        @Test
        @DisplayName("팀원이 없으면 빈 리더보드를 반환한다")
        void noMembers_returnsEmptyLeaderboard() {
            // given
            given(teamMemberRepository.findByTeamIdWithMember(TEAM_ID)).willReturn(List.of());
            given(squadRepository.findByTeamIdOrderByIdAsc(TEAM_ID)).willReturn(List.of());

            // when
            TeamLeaderboardResponse response = teamActivityService.getTeamLeaderboard(TEAM_ID, MEMBER_ID, 30);

            // then
            assertThat(response).isNotNull();
            assertThat(response.memberRanks()).isEmpty();
        }

        @Test
        @DisplayName("추천이 없으면 모든 멤버가 0문제로 동률 1위")
        void noRecommendations_allMembersRankFirst() {
            // given
            given(teamMemberRepository.findByTeamIdWithMember(TEAM_ID))
                    .willReturn(List.of(teamMember1, teamMember2));
            given(squadRepository.findByTeamIdOrderByIdAsc(TEAM_ID)).willReturn(List.of());
            given(memberRecommendationRepository.findByTeamIdAndCreatedAtBetween(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of());

            // when
            TeamLeaderboardResponse response = teamActivityService.getTeamLeaderboard(TEAM_ID, MEMBER_ID, 30);

            // then
            assertThat(response.memberRanks()).hasSize(2);
            assertThat(response.memberRanks()).allSatisfy(rank ->
                    assertThat(rank.rank()).isEqualTo(1)
            );
        }
    }
}
