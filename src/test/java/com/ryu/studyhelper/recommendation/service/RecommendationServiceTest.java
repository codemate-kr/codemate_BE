package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.dto.response.MyTodayProblemsResponse;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ProblemTagRepository problemTagRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationProblemRepository recommendationProblemRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @Mock
    private RecommendationCreator recommendationCreator;

    @Mock
    private RecommendationEmailService recommendationEmailService;

    private RecommendationService recommendationService;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Long TEAM_ID = 1L;
    private static final Long MEMBER_ID = 100L;

    private Clock fixedClock(String dateTime) {
        LocalDateTime ldt = LocalDateTime.parse(dateTime);
        Instant instant = ldt.atZone(ZONE_ID).toInstant();
        return Clock.fixed(instant, ZONE_ID);
    }

    private void setupServiceWithClock(Clock clock) {
        recommendationService = new RecommendationService(
                clock,
                teamRepository,
                teamMemberRepository,
                problemTagRepository,
                recommendationRepository,
                recommendationProblemRepository,
                memberRecommendationRepository,
                recommendationCreator,
                recommendationEmailService
        );
    }

    @Nested
    @DisplayName("수동 추천 금지 시간대 검증")
    class BlockedTimeValidation {

        @ParameterizedTest
        @CsvSource({
                "2025-01-15T05:00:00",
                "2025-01-15T05:30:00",
                "2025-01-15T06:59:59"
        })
        @DisplayName("오전 5시~7시 사이에는 수동 추천 생성이 금지된다")
        void blockedTime_throwsException(String dateTime) {
            // given
            Clock clock = fixedClock(dateTime);
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명", false);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_BLOCKED_TIME);
                    });
        }

        @ParameterizedTest
        @CsvSource({
                "2025-01-15T04:59:59",
                "2025-01-15T07:00:00",
                "2025-01-15T07:00:01",
                "2025-01-15T10:00:00",
                "2025-01-15T23:59:59"
        })
        @DisplayName("금지 시간대 외에는 시간 검증을 통과한다")
        void allowedTime_passesTimeValidation(String dateTime) {
            // given
            Clock clock = fixedClock(dateTime);
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.empty());
            when(recommendationCreator.create(any(), any()))
                    .thenReturn(createRecommendationWithCreatedAt(TEAM_ID, LocalDateTime.now()));

            // when: 시간 검증 통과하여 정상 실행
            recommendationService.createManualRecommendation(TEAM_ID);

            // then: Creator가 호출되었음을 검증 (시간 검증 통과)
            verify(recommendationCreator).create(any(), eq(RecommendationType.MANUAL));
        }
    }

    @Nested
    @DisplayName("미션 사이클 기반 중복 추천 검증")
    class MissionCycleValidation {

        @Test
        @DisplayName("현재 미션 사이클 내 추천이 있으면 수동 추천이 금지된다")
        void existingRecommendationInCycle_throwsException() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명", false);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T06:30:00")
            );
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    });
        }

        @Test
        @DisplayName("이전 미션 사이클의 추천은 현재 사이클에 영향을 주지 않는다")
        void previousCycleRecommendation_allowsNewRecommendation() {
            // given: 오전 10시 (2025-01-15), 미션 사이클: 01-15 06:00 ~
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // 어제 10시 추천 (이전 사이클)
            Recommendation oldRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-14T10:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(oldRecommendation));
            when(recommendationCreator.create(any(), any()))
                    .thenReturn(createRecommendationWithCreatedAt(TEAM_ID, LocalDateTime.now()));

            // when: 사이클 검증 통과하여 정상 실행
            recommendationService.createManualRecommendation(TEAM_ID);

            // then
            verify(recommendationCreator).create(any(), eq(RecommendationType.MANUAL));
        }

        @Test
        @DisplayName("오전 6시 전에는 전날 06:00부터가 미션 사이클이다")
        void before6AM_usesYesterdayCycleStart() {
            // given: 새벽 3시 30분 (2025-01-15)
            // 미션 사이클: 2025-01-14 06:00 ~ 2025-01-15 06:00
            Clock clock = fixedClock("2025-01-15T03:30:00");
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명", false);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // 2025-01-14 07:00에 생성된 추천 (현재 사이클)
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-14T07:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    });
        }

        @Test
        @DisplayName("정확히 06:00에 생성된 추천도 현재 사이클에 포함된다")
        void exactlyAt6AM_isIncludedInCurrentCycle() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명", false);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // 정확히 오늘 06:00:00에 생성된 추천
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T06:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    });
        }
    }

    @Nested
    @DisplayName("수동 추천 problemCount 검증")
    class ManualRecommendationProblemCountValidation {

        @Test
        @DisplayName("팀 설정의 problemCount를 사용하여 추천을 생성한다")
        void usesTeamProblemCount() {
            // given: 오전 10시 (금지 시간대 외)
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            // problemCount가 5로 설정된 팀
            Team team = createTeamWithIdAndProblemCount(TEAM_ID, 5);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.empty());
            when(recommendationCreator.create(any(), any()))
                    .thenReturn(createRecommendationWithCreatedAt(TEAM_ID, LocalDateTime.now()));

            // when
            recommendationService.createManualRecommendation(TEAM_ID);

            // then: Creator가 team 객체(problemCount=5)와 MANUAL 타입으로 호출됨
            verify(recommendationCreator).create(team, RecommendationType.MANUAL);
        }
    }

    @Nested
    @DisplayName("내 오늘의 문제 전체 조회 (getMyTodayProblems)")
    class GetMyTodayProblemsValidation {

        @Test
        @DisplayName("팀에 속하지 않은 유저는 빈 목록을 반환한다")
        void noTeams_returnsEmptyList() {
            // given
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            when(teamMemberRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of());

            // when
            MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(MEMBER_ID);

            // then
            assertThat(response.teams()).isEmpty();
            assertThat(response.totalProblemCount()).isZero();
            assertThat(response.solvedCount()).isZero();
        }

        @Test
        @DisplayName("팀에 속해있지만 오늘 추천이 없으면 빈 목록을 반환한다")
        void teamsWithNoRecommendation_returnsEmptyList() {
            // given
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            TeamMember teamMember = createTeamMember(team, MEMBER_ID);

            when(teamMemberRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(teamMember));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.empty());

            // when
            MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(MEMBER_ID);

            // then
            assertThat(response.teams()).isEmpty();
        }

        @Test
        @DisplayName("팀에 오늘 추천이 있으면 해당 팀의 문제를 반환한다")
        void teamWithRecommendation_returnsProblems() {
            // given
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithIdAndName(TEAM_ID, "알고리즘 스터디");
            TeamMember teamMember = createTeamMember(team, MEMBER_ID);

            Recommendation recommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T06:00:00")
            );
            setRecommendationId(recommendation, 42L);

            when(teamMemberRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(teamMember));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(recommendation));
            lenient().when(recommendationProblemRepository.findProblemsWithSolvedStatus(42L, MEMBER_ID))
                    .thenReturn(List.of());
            lenient().when(problemTagRepository.findTagsByProblemIds(any())).thenReturn(List.of());

            // when
            MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(MEMBER_ID);

            // then
            assertThat(response.teams()).hasSize(1);
            assertThat(response.teams().get(0).teamId()).isEqualTo(TEAM_ID);
            assertThat(response.teams().get(0).teamName()).isEqualTo("알고리즘 스터디");
            assertThat(response.teams().get(0).recommendationId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("여러 팀에 속한 유저는 모든 팀의 오늘 문제를 반환한다")
        void multipleTeams_returnsAllTeamsProblems() {
            // given
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team1 = createTeamWithIdAndName(TEAM_ID, "팀1");
            Team team2 = createTeamWithIdAndName(2L, "팀2");
            TeamMember teamMember1 = createTeamMember(team1, MEMBER_ID);
            TeamMember teamMember2 = createTeamMember(team2, MEMBER_ID);

            Recommendation recommendation1 = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-15T06:00:00"));
            setRecommendationId(recommendation1, 42L);

            Recommendation recommendation2 = createRecommendationWithCreatedAt(
                    2L, LocalDateTime.parse("2025-01-15T06:00:00"));
            setRecommendationId(recommendation2, 43L);

            when(teamMemberRepository.findByMemberId(MEMBER_ID))
                    .thenReturn(List.of(teamMember1, teamMember2));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(recommendation1));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(2L))
                    .thenReturn(Optional.of(recommendation2));
            lenient().when(recommendationProblemRepository.findProblemsWithSolvedStatus(any(), eq(MEMBER_ID)))
                    .thenReturn(List.of());
            lenient().when(problemTagRepository.findTagsByProblemIds(any())).thenReturn(List.of());

            // when
            MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(MEMBER_ID);

            // then
            assertThat(response.teams()).hasSize(2);
        }

        @Test
        @DisplayName("이전 미션 사이클의 추천은 오늘 문제에 포함되지 않는다")
        void previousCycleRecommendation_notIncluded() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            TeamMember teamMember = createTeamMember(team, MEMBER_ID);

            // 어제 추천 (이전 미션 사이클)
            Recommendation oldRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-14T10:00:00"));

            when(teamMemberRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(teamMember));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.of(oldRecommendation));

            // when
            MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(MEMBER_ID);

            // then: 이전 사이클 추천은 필터링됨
            assertThat(response.teams()).isEmpty();
        }

    }

    // === Helper Methods ===

    private Team createTeamWithId(Long id) {
        return createTeamWithIdAndName(id, "테스트팀");
    }

    private Team createTeamWithIdAndName(Long id, String name) {
        Team team = Team.create(name, "설명", false);
        setFieldValue(team, "id", id);
        return team;
    }

    private Team createTeamWithIdAndProblemCount(Long id, int problemCount) {
        Team team = createTeamWithId(id);
        team.updateProblemCount(problemCount);
        return team;
    }

    private TeamMember createTeamMember(Team team, Long memberId) {
        Member member = Member.builder()
                .email("test@test.com")
                .provider("google")
                .providerId("test-provider-id")
                .isVerified(false)
                .build();
        setFieldValue(member, "id", memberId);
        return TeamMember.create(team, member, TeamRole.MEMBER);
    }

    private Recommendation createRecommendationWithCreatedAt(Long teamId, LocalDateTime createdAt) {
        Recommendation recommendation = Recommendation.createScheduledRecommendation(teamId);
        setFieldValue(recommendation, "createdAt", createdAt, true);
        return recommendation;
    }

    private void setRecommendationId(Recommendation recommendation, Long id) {
        setFieldValue(recommendation, "id", id);
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        setFieldValue(target, fieldName, value, false);
    }

    private void setFieldValue(Object target, String fieldName, Object value, boolean superClass) {
        try {
            Class<?> clazz = superClass ? target.getClass().getSuperclass() : target.getClass();
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 설정 실패", e);
        }
    }
}
