package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.member.repository.MemberSolvedProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.dto.projection.MemberSolvedSummaryProjection;
import com.ryu.studyhelper.team.dto.response.TeamActivityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamActivityService 단위 테스트")
class TeamActivityServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private MemberSolvedProblemRepository memberSolvedProblemRepository;

    @InjectMocks
    private TeamActivityService teamActivityService;

    private static final Long TEAM_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    private Team publicTeam;
    private Team privateTeam;
    private Member member1;
    private Member member2;

    @BeforeEach
    void setUp() {
        publicTeam = Team.create("공개 팀", "공개 팀 설명", false);
        setId(publicTeam, TEAM_ID);

        privateTeam = Team.create("비공개 팀", "비공개 팀 설명", true);
        setId(privateTeam, TEAM_ID);

        member1 = Member.builder()
                .id(1L)
                .email("member1@test.com")
                .handle("handle1")
                .provider("google")
                .providerId("provider1")
                .role(Role.ROLE_USER)
                .isVerified(true)
                .build();

        member2 = Member.builder()
                .id(2L)
                .email("member2@test.com")
                .handle("handle2")
                .provider("google")
                .providerId("provider2")
                .role(Role.ROLE_USER)
                .isVerified(true)
                .build();
    }

    @Nested
    @DisplayName("팀 접근 권한 검증")
    class TeamAccessValidation {

        @Test
        @DisplayName("공개 팀은 비로그인 사용자도 조회 가능하다")
        void publicTeam_allowsAnonymousAccess() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID)).willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response).isNotNull();
            assertThat(response.currentMemberId()).isNull();
        }

        @Test
        @DisplayName("공개 팀은 로그인한 사용자도 조회 가능하다")
        void publicTeam_allowsAuthenticatedAccess() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID)).willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, MEMBER_ID, 30);

            // then
            assertThat(response).isNotNull();
            assertThat(response.currentMemberId()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("비공개 팀은 비로그인 사용자가 접근할 수 없다")
        void privateTeam_deniesAnonymousAccess() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(privateTeam));

            // when & then
            assertThatThrownBy(() -> teamActivityService.getTeamActivity(TEAM_ID, null, 30))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("비공개 팀은 팀원이 아닌 사용자가 접근할 수 없다")
        void privateTeam_deniesNonMemberAccess() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(privateTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberId(TEAM_ID, MEMBER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> teamActivityService.getTeamActivity(TEAM_ID, MEMBER_ID, 30))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("비공개 팀은 팀원만 조회 가능하다")
        void privateTeam_allowsMemberAccess() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(privateTeam));
            given(teamMemberRepository.existsByTeamIdAndMemberId(TEAM_ID, MEMBER_ID)).willReturn(true);
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID)).willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, MEMBER_ID, 30);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 팀 조회 시 예외 발생")
        void nonExistentTeam_throwsException() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> teamActivityService.getTeamActivity(TEAM_ID, MEMBER_ID, 30))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.TEAM_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("days 파라미터 검증")
    class DaysParameterValidation {

        @BeforeEach
        void setUp() {
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID)).willReturn(List.of());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("null, 0, 음수일 때 기본값 30일이 적용된다")
        void invalidDays_usesDefaultValue(Integer days) {
            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, days);

            // then
            assertThat(response.period().days()).isEqualTo(30);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 7, 15, 30})
        @DisplayName("유효한 days 값은 그대로 사용된다")
        void validDays_usesProvidedValue(int days) {
            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, days);

            // then
            assertThat(response.period().days()).isEqualTo(days);
        }

        @ParameterizedTest
        @ValueSource(ints = {31, 50, 100})
        @DisplayName("30일 초과 시 최대값 30일이 적용된다")
        void exceedingMaxDays_usesMaxValue(int days) {
            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, days);

            // then
            assertThat(response.period().days()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("빈 데이터 처리")
    class EmptyDataHandling {

        @Test
        @DisplayName("팀원이 없으면 빈 응답을 반환한다")
        void noMembers_returnsEmptyResponse() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID)).willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).isEmpty();
            assertThat(response.dailyActivities()).isEmpty();
            verify(recommendationRepository, never()).findByTeamIdAndCreatedAtBetweenWithProblems(any(), any(), any());
        }

        @Test
        @DisplayName("추천이 없으면 모든 멤버가 0문제로 동률 1위")
        void noRecommendations_allMembersRankFirst() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1, member2));
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).hasSize(2);
            assertThat(response.members()).allSatisfy(memberRank -> {
                assertThat(memberRank.rank()).isEqualTo(1);
                assertThat(memberRank.totalSolved()).isEqualTo(0);
            });
            assertThat(response.dailyActivities()).isEmpty();
        }

        @Test
        @DisplayName("추천이 없어도 멤버의 handle이 반환된다")
        void noRecommendations_membersHaveHandles() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1, member2));
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).hasSize(2);
            assertThat(response.members())
                    .extracting(TeamActivityResponse.MemberRank::handle)
                    .containsExactlyInAnyOrder("handle1", "handle2");
        }

        @Test
        @DisplayName("추천이 없을 때 handle이 null인 멤버도 정상 처리된다")
        void noRecommendations_nullHandleMemberIncluded() {
            // given
            Member memberWithNullHandle = Member.builder()
                    .id(3L)
                    .email("nohandle@test.com")
                    .handle(null)  // BOJ 인증 안 함
                    .provider("google")
                    .providerId("provider3")
                    .role(Role.ROLE_USER)
                    .isVerified(false)
                    .build();

            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1, memberWithNullHandle));
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).hasSize(2);
            // handle 기준 정렬: handle1이 먼저, null은 마지막
            assertThat(response.members().get(0).handle()).isEqualTo("handle1");
            assertThat(response.members().get(1).handle()).isNull();
            assertThat(response.members().get(1).memberId()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("리더보드 순위 계산")
    class LeaderboardRanking {

        @Test
        @DisplayName("풀이 수에 따라 올바른 순위가 부여된다")
        void ranksBasedOnSolvedCount() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1, member2));

            Recommendation recommendation = createRecommendationWithProblems(TEAM_ID, 3);
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of(recommendation));

            // member1: 3문제 풀이, member2: 1문제 풀이
            given(memberSolvedProblemRepository.countSolvedByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of(
                            createProjection(1L, "handle1", 3L),
                            createProjection(2L, "handle2", 1L)
                    ));

            given(memberSolvedProblemRepository.findByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).hasSize(2);
            assertThat(response.members().get(0).rank()).isEqualTo(1);
            assertThat(response.members().get(0).totalSolved()).isEqualTo(3);
            assertThat(response.members().get(1).rank()).isEqualTo(2);
            assertThat(response.members().get(1).totalSolved()).isEqualTo(1);
        }

        @Test
        @DisplayName("동점자는 같은 순위를 부여받고 다음 순위는 건너뛴다")
        void tiedMembersShareRank() {
            // given
            Member member3 = Member.builder()
                    .id(3L)
                    .email("member3@test.com")
                    .handle("handle3")
                    .provider("google")
                    .providerId("provider3")
                    .role(Role.ROLE_USER)
                    .isVerified(true)
                    .build();

            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1, member2, member3));

            Recommendation recommendation = createRecommendationWithProblems(TEAM_ID, 5);
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of(recommendation));

            // member1: 5문제, member2: 3문제, member3: 3문제 (공동 2위)
            given(memberSolvedProblemRepository.countSolvedByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of(
                            createProjection(1L, "handle1", 5L),
                            createProjection(2L, "handle2", 3L),
                            createProjection(3L, "handle3", 3L)
                    ));

            given(memberSolvedProblemRepository.findByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of());

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.members()).hasSize(3);
            assertThat(response.members().get(0).rank()).isEqualTo(1);  // 5문제
            assertThat(response.members().get(1).rank()).isEqualTo(2);  // 3문제 (공동 2위)
            assertThat(response.members().get(2).rank()).isEqualTo(2);  // 3문제 (공동 2위)
            // 다음 순위는 4위 (3위 건너뜀)
        }
    }

    @Nested
    @DisplayName("일별 활동 조회")
    class DailyActivityQuery {

        @Test
        @DisplayName("추천된 문제와 멤버별 풀이 현황이 포함된다")
        void includesProblemInfoAndMemberSolvedStatus() {
            // given
            given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(publicTeam));
            given(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .willReturn(List.of(member1));

            Recommendation recommendation = createRecommendationWithProblems(TEAM_ID, 2);
            given(recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(eq(TEAM_ID), any(), any()))
                    .willReturn(List.of(recommendation));

            given(memberSolvedProblemRepository.countSolvedByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of(createProjection(1L, "handle1", 1L)));

            // member1이 첫 번째 문제만 풀었음
            Problem problem1 = recommendation.getProblems().get(0).getProblem();
            MemberSolvedProblem solvedProblem = MemberSolvedProblem.builder()
                    .member(member1)
                    .problem(problem1)
                    .solvedAt(LocalDateTime.now())
                    .build();
            given(memberSolvedProblemRepository.findByMemberIdsAndProblemIds(anyList(), anyList()))
                    .willReturn(List.of(solvedProblem));

            // when
            TeamActivityResponse response = teamActivityService.getTeamActivity(TEAM_ID, null, 30);

            // then
            assertThat(response.dailyActivities()).hasSize(1);
            TeamActivityResponse.DailyActivity activity = response.dailyActivities().get(0);
            assertThat(activity.problems()).hasSize(2);
            assertThat(activity.memberSolved()).hasSize(1);

            // 첫 번째 문제는 풀었고, 두 번째는 안 풀었음
            TeamActivityResponse.MemberSolved memberSolved = activity.memberSolved().get(0);
            assertThat(memberSolved.memberId()).isEqualTo(1L);
            assertThat(memberSolved.solved().get(problem1.getId())).isTrue();
        }
    }

    // ========== 헬퍼 메서드 ==========

    private void setId(Team team, Long id) {
        try {
            java.lang.reflect.Field idField = team.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(team, id);
        } catch (Exception e) {
            throw new RuntimeException("id 설정 실패", e);
        }
    }

    private Recommendation createRecommendationWithProblems(Long teamId, int problemCount) {
        Recommendation recommendation = Recommendation.createScheduledRecommendation(teamId);
        setCreatedAt(recommendation, LocalDateTime.now().minusDays(1));

        for (int i = 1; i <= problemCount; i++) {
            Problem problem = createProblem((long) i, "문제 " + i, 10 + i);
            RecommendationProblem rp = RecommendationProblem.create(problem);
            setRecommendationProblemId(rp, (long) i);
            recommendation.addProblem(rp);
        }

        return recommendation;
    }

    private void setRecommendationProblemId(RecommendationProblem rp, Long id) {
        try {
            java.lang.reflect.Field idField = rp.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(rp, id);
        } catch (Exception e) {
            throw new RuntimeException("RecommendationProblem id 설정 실패", e);
        }
    }

    private Problem createProblem(Long id, String title, Integer level) {
        return Problem.builder()
                .id(id)
                .title(title)
                .titleKo(title)
                .level(level)
                .acceptedUserCount(100)
                .averageTries(2.5)
                .build();
    }

    private void setCreatedAt(Recommendation recommendation, LocalDateTime createdAt) {
        try {
            java.lang.reflect.Field createdAtField = recommendation.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(recommendation, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("createdAt 설정 실패", e);
        }
    }

    private MemberSolvedSummaryProjection createProjection(Long memberId, String handle, Long totalSolved) {
        return new MemberSolvedSummaryProjection() {
            @Override
            public Long getMemberId() {
                return memberId;
            }

            @Override
            public String getHandle() {
                return handle;
            }

            @Override
            public Long getTotalSolved() {
                return totalSolved;
            }
        };
    }
}