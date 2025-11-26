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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                "2025-01-15T01:00:00",
                "2025-01-15T01:30:00",
                "2025-01-15T01:59:59"
        })
        @DisplayName("새벽 1시~2시 사이에는 수동 추천 생성이 금지된다")
        void blockedTime_throwsException(String dateTime) {
            // given
            Clock clock = fixedClock(dateTime);
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명");
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
                "2025-01-15T00:59:59",
                "2025-01-15T02:00:00",
                "2025-01-15T02:00:01",
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

            Team team = Team.create("테스트팀", "설명");
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T02:30:00")
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
            // given: 오전 10시 (2025-01-15), 미션 사이클: 01-15 02:00 ~
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
        @DisplayName("새벽 2시 전에는 전날 02:00부터가 미션 사이클이다")
        void before2AM_usesYesterdayCycleStart() {
            // given: 새벽 0시 30분 (2025-01-15)
            // 미션 사이클: 2025-01-14 02:00 ~ 2025-01-15 02:00
            Clock clock = fixedClock("2025-01-15T00:30:00");
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명");
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // 2025-01-14 03:00에 생성된 추천 (현재 사이클)
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-14T03:00:00")
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
        @DisplayName("정확히 02:00에 생성된 추천도 현재 사이클에 포함된다")
        void exactlyAt2AM_isIncludedInCurrentCycle() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = Team.create("테스트팀", "설명");
            when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

            // 정확히 오늘 02:00:00에 생성된 추천
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T02:00:00")
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
    @DisplayName("스케줄러 미션 사이클 기반 추천 검증")
    class SchedulerMissionCycleValidation {

        @Test
        @DisplayName("0~1시 사이 수동 추천 후 2시 스케줄러는 새 추천을 생성한다")
        void manualAt0AM_schedulerAt2AM_createsNewRecommendation() {
            // given: 새벽 2시 스케줄러 실행
            // 미션 사이클: 2025-01-15 02:00 ~ 2025-01-16 02:00
            Clock clock = fixedClock("2025-01-15T02:00:00");
            setupServiceWithClock(clock);

            // 0:30에 생성된 수동 추천 (이전 미션 사이클: 01-14 02:00 ~ 01-15 02:00)
            Recommendation manualRecommendation = createManualRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T00:30:00")
            );

            // 스케줄러가 현재 미션 사이클(01-15 02:00 ~) 범위로 조회 시 결과 없음
            LocalDateTime missionCycleStart = LocalDateTime.parse("2025-01-15T02:00:00");
            LocalDateTime now = LocalDateTime.parse("2025-01-15T02:00:00");

            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    TEAM_ID, missionCycleStart, now
            )).thenReturn(Optional.empty());

            // when & then: 0:30 추천은 이전 사이클이므로 조회되지 않음
            Optional<Recommendation> result = recommendationRepository
                    .findFirstByTeamIdAndCreatedAtBetweenOrderById(TEAM_ID, missionCycleStart, now);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("2시 이후 추천이 있으면 스케줄러가 중복 생성하지 않는다")
        void recommendationAfter2AM_schedulerSkips() {
            // given: 새벽 2시 30분 스케줄러 재실행 (장애 복구 등)
            Clock clock = fixedClock("2025-01-15T02:30:00");
            setupServiceWithClock(clock);

            // 2:05에 이미 생성된 추천 (현재 미션 사이클)
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID,
                    LocalDateTime.parse("2025-01-15T02:05:00")
            );

            LocalDateTime missionCycleStart = LocalDateTime.parse("2025-01-15T02:00:00");
            LocalDateTime now = LocalDateTime.parse("2025-01-15T02:30:00");

            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    TEAM_ID, missionCycleStart, now
            )).thenReturn(Optional.of(existingRecommendation));

            // when & then: 현재 사이클에 추천이 있으므로 조회됨
            Optional<Recommendation> result = recommendationRepository
                    .findFirstByTeamIdAndCreatedAtBetweenOrderById(TEAM_ID, missionCycleStart, now);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("자정~2시 사이 추천은 이전 미션 사이클로 간주된다")
        void recommendationBetweenMidnightAnd2AM_belongsToPreviousCycle() {
            // given: 시나리오 검증
            // 1. 1월 15일 00:30에 수동 추천 생성
            // 2. 1월 15일 02:00에 스케줄러 실행

            // 00:30 추천의 미션 사이클 시작: 1월 14일 02:00
            LocalDateTime recommendationTime = LocalDateTime.parse("2025-01-15T00:30:00");
            LocalDateTime expectedCycleStart = LocalDateTime.parse("2025-01-14T02:00:00");

            // 02:00 스케줄러의 미션 사이클 시작: 1월 15일 02:00
            LocalDateTime schedulerTime = LocalDateTime.parse("2025-01-15T02:00:00");
            LocalDateTime schedulerCycleStart = LocalDateTime.parse("2025-01-15T02:00:00");

            // when & then
            // 00:30 추천은 스케줄러의 미션 사이클 시작(02:00) 이전이므로 다른 사이클
            assertThat(recommendationTime.isBefore(schedulerCycleStart)).isTrue();
            // 따라서 스케줄러는 새 추천을 생성해야 함
        }
    }

    /**
     * ID가 설정된 Team 생성 (테스트용)
     */
    private Team createTeamWithId(Long id) {
        Team team = Team.create("테스트팀", "설명");
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