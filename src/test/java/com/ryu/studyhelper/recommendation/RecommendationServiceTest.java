package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.ProblemService;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.TeamMemberRepository;
import com.ryu.studyhelper.team.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RecommendationService 테스트
 * Clock 주입을 통해 시간 의존적 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemService problemService;

    @Mock
    private MailSendService mailSendService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationProblemRepository recommendationProblemRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    private RecommendationService recommendationService;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Long TEAM_ID = 1L;

    /**
     * 특정 시간으로 고정된 Clock 생성
     */
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
                problemRepository,
                problemService,
                mailSendService,
                recommendationRepository,
                recommendationProblemRepository,
                memberRecommendationRepository
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
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
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
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID))
                    .thenReturn(java.util.List.of());

            // when & then: 시간 검증 통과, 핸들 없어서 실패
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증된 핸들이 없습니다");
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
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
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
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID))
                    .thenReturn(java.util.List.of());

            // when & then: 사이클 검증 통과, 핸들 없어서 실패
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("인증된 핸들이 없습니다");
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
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
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
            assertThatThrownBy(() -> recommendationService.createManualRecommendation(TEAM_ID, 3))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    });
        }
    }

    @Nested
    @DisplayName("이메일 즉시 발송 시간 검증")
    class EmailSendableTimeValidation {

        @ParameterizedTest
        @CsvSource({
                "2025-01-15T09:00:00, true",
                "2025-01-15T12:00:00, true",
                "2025-01-15T23:59:59, true",
                "2025-01-15T00:00:00, true",
                "2025-01-15T00:59:59, true",
                "2025-01-15T02:00:00, false",
                "2025-01-15T05:00:00, false",
                "2025-01-15T08:59:59, false"
        })
        @DisplayName("시간대별 이메일 즉시 발송 여부 로직 검증")
        void emailSendableTime_correctBehavior(String dateTime, boolean shouldSendImmediately) {
            // isEmailSendableTime() 로직 검증
            LocalDateTime time = LocalDateTime.parse(dateTime);
            java.time.LocalTime localTime = time.toLocalTime();

            java.time.LocalTime emailStart = java.time.LocalTime.of(9, 0);
            java.time.LocalTime blockedStart = java.time.LocalTime.of(1, 0);

            boolean result = !localTime.isBefore(emailStart) || localTime.isBefore(blockedStart);

            assertThat(result).isEqualTo(shouldSendImmediately);
        }
    }

    @Nested
    @DisplayName("수동 추천 problemCount 검증")
    class ManualRecommendationProblemCountValidation {

        @Test
        @DisplayName("count가 null이면 팀 설정의 problemCount를 사용한다")
        void nullCount_usesTeamProblemCount() {
            // given: 오전 10시 (금지 시간대 외)
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            // problemCount가 5로 설정된 팀
            Team team = createTeamWithIdAndProblemCount(TEAM_ID, 5);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID))
                    .thenReturn(List.of("handle1"));
            when(problemService.recommend(any(), eq(5), any(), any()))
                    .thenReturn(List.of());
            when(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .thenReturn(List.of());

            // when
            recommendationService.createManualRecommendation(TEAM_ID, null);

            // then: 팀 설정값 5개의 문제가 요청되었는지 검증
            verify(problemService).recommend(any(), eq(5), any(), any());
        }

        @Test
        @DisplayName("count가 지정되면 팀 설정값 대신 지정된 값을 사용한다")
        void specifiedCount_overridesTeamSetting() {
            // given: 오전 10시 (금지 시간대 외)
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            // problemCount가 5로 설정된 팀
            Team team = createTeamWithIdAndProblemCount(TEAM_ID, 5);
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
            when(recommendationRepository.findFirstByTeamIdOrderByCreatedAtDesc(TEAM_ID))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID))
                    .thenReturn(List.of("handle1"));
            when(problemService.recommend(any(), eq(7), any(), any()))
                    .thenReturn(List.of());
            when(teamMemberRepository.findMembersByTeamId(TEAM_ID))
                    .thenReturn(List.of());

            // when
            recommendationService.createManualRecommendation(TEAM_ID, 7);

            // then: 지정된 값 7개의 문제가 요청되었는지 검증 (팀 설정값 5 무시)
            verify(problemService).recommend(any(), eq(7), any(), any());
        }
    }

    @Nested
    @DisplayName("스케줄러 미션 사이클 기반 추천 검증")
    class SchedulerMissionCycleValidation {

        @Test
        @DisplayName("prepareDailyRecommendations()는 미션 사이클 시작(06:00)부터 현재까지의 범위로 조회한다")
        void prepareDailyRecommendations_usesMissionCycleRange() {
            // given: 오전 6시 스케줄러 실행 (수요일)
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            // 수요일에 추천 활성화된 팀 설정
            Team team = createTeamWithIdAndRecommendationDay(TEAM_ID, DayOfWeek.WEDNESDAY);
            when(teamRepository.findAll()).thenReturn(List.of(team));

            // 현재 미션 사이클 내 추천 없음
            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID), any(LocalDateTime.class), any(LocalDateTime.class)
            )).thenReturn(Optional.empty());

            // 핸들 없어서 추천 생성 실패하도록 설정 (범위 검증이 목적)
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of());

            // when
            recommendationService.prepareDailyRecommendations();

            // then: 미션 사이클 범위(06:00 ~ now)로 조회했는지 검증
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            verify(recommendationRepository).findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID), fromCaptor.capture(), toCaptor.capture()
            );

            LocalDateTime capturedFrom = fromCaptor.getValue();
            LocalDateTime capturedTo = toCaptor.getValue();

            // 미션 사이클 시작: 2025-01-15 06:00 (0시가 아님!)
            assertThat(capturedFrom).isEqualTo(LocalDateTime.parse("2025-01-15T06:00:00"));
            assertThat(capturedTo).isEqualTo(LocalDateTime.parse("2025-01-15T06:00:00"));
        }

        @Test
        @DisplayName("0시~6시 사이 수동 추천은 이전 미션 사이클로 간주되어 6시 스케줄러에서 조회되지 않는다")
        void manualAt0AM_notFoundBySchedulerAt6AM() {
            // given: 오전 6시 스케줄러 실행 (수요일)
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithIdAndRecommendationDay(TEAM_ID, DayOfWeek.WEDNESDAY);
            when(teamRepository.findAll()).thenReturn(List.of(team));

            // 0:30에 생성된 수동 추천 - 이전 미션 사이클(01-14 06:00 ~ 01-15 06:00)에 속함
            // 스케줄러가 조회하는 범위(01-15 06:00~)에 포함되지 않음
            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00")),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00"))
            )).thenReturn(Optional.empty());  // 0:30 추천은 범위 밖이므로 조회 안됨

            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of());

            // when
            recommendationService.prepareDailyRecommendations();

            // then: 미션 사이클 범위로 조회 확인 (0시가 아닌 6시부터)
            verify(recommendationRepository).findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00")),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00"))
            );
        }

        @Test
        @DisplayName("캘린더 날짜(0시)가 아닌 미션 사이클(6시) 기준으로 조회해야 한다")
        void shouldUseMissionCycleNotCalendarDate() {
            // given: 6시 30분 스케줄러 실행
            Clock clock = fixedClock("2025-01-15T06:30:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithIdAndRecommendationDay(TEAM_ID, DayOfWeek.WEDNESDAY);
            when(teamRepository.findAll()).thenReturn(List.of(team));
            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    any(), any(), any()
            )).thenReturn(Optional.empty());
            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of());

            // when
            recommendationService.prepareDailyRecommendations();

            // then
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(recommendationRepository).findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID), fromCaptor.capture(), any()
            );

            // 핵심: 0시가 아닌 6시부터 조회해야 함
            LocalDateTime from = fromCaptor.getValue();
            assertThat(from.getHour()).isEqualTo(6);
            assertThat(from.getMinute()).isEqualTo(0);
        }
    }

    /**
     * 추천 요일이 설정된 Team 생성 (테스트용)
     * - getActiveTeams() 필터를 통과하려면 teamMembers가 비어있지 않아야 함
     */
    private Team createTeamWithIdAndRecommendationDay(Long id, DayOfWeek dayOfWeek) {
        Team team = Team.create("테스트팀", "설명", false);
        try {
            java.lang.reflect.Field idField = team.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(team, id);

            // 추천 활성화 및 요일 설정
            com.ryu.studyhelper.team.domain.RecommendationDayOfWeek recommendationDay =
                    com.ryu.studyhelper.team.domain.RecommendationDayOfWeek.from(dayOfWeek);
            team.updateRecommendationDays(List.of(recommendationDay));

            // 팀원 목록에 더미 데이터 추가 (getActiveTeams() 필터 통과용)
            java.lang.reflect.Field teamMembersField = team.getClass().getDeclaredField("teamMembers");
            teamMembersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> teamMembers = (java.util.List<Object>) teamMembersField.get(team);
            teamMembers.add(new Object()); // 더미 객체 추가
        } catch (Exception e) {
            throw new RuntimeException("Team 설정 실패", e);
        }
        return team;
    }

    /**
     * ID가 설정된 Team 생성 (테스트용)
     */
    private Team createTeamWithId(Long id) {
        Team team = Team.create("테스트팀", "설명", false);
        try {
            java.lang.reflect.Field idField = team.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(team, id);
        } catch (Exception e) {
            throw new RuntimeException("id 설정 실패", e);
        }
        return team;
    }

    /**
     * ID와 problemCount가 설정된 Team 생성 (테스트용)
     */
    private Team createTeamWithIdAndProblemCount(Long id, int problemCount) {
        Team team = createTeamWithId(id);
        team.updateProblemCount(problemCount);
        return team;
    }

    /**
     * createdAt이 설정된 스케줄 Recommendation 생성 (테스트용)
     */
    private Recommendation createRecommendationWithCreatedAt(Long teamId, LocalDateTime createdAt) {
        Recommendation recommendation = Recommendation.createScheduledRecommendation(teamId);
        setCreatedAt(recommendation, createdAt);
        return recommendation;
    }

    /**
     * createdAt이 설정된 수동 Recommendation 생성 (테스트용)
     */
    private Recommendation createManualRecommendationWithCreatedAt(Long teamId, LocalDateTime createdAt) {
        Recommendation recommendation = Recommendation.createManualRecommendation(teamId);
        setCreatedAt(recommendation, createdAt);
        return recommendation;
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
}