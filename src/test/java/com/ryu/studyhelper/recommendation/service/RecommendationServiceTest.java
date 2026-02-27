package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService 테스트")
class RecommendationServiceTest {

    @Mock
    private SquadRepository squadRepository;

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
    private static final Long SQUAD_ID = 10L;

    private Clock fixedClock(String dateTime) {
        LocalDateTime ldt = LocalDateTime.parse(dateTime);
        Instant instant = ldt.atZone(ZONE_ID).toInstant();
        return Clock.fixed(instant, ZONE_ID);
    }

    private void setupServiceWithClock(Clock clock) {
        recommendationService = new RecommendationService(
                clock,
                squadRepository,
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
        @DisplayName("오전 5시~7시 사이에는 스쿼드 수동 추천 생성이 금지된다")
        void blockedTime_throwsException(String dateTime) {
            // given
            Clock clock = fixedClock(dateTime);
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID))
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
                "2025-01-15T10:00:00",
                "2025-01-15T23:59:59"
        })
        @DisplayName("금지 시간대 외에는 시간 검증을 통과한다")
        void allowedTime_passesTimeValidation(String dateTime) {
            // given
            Clock clock = fixedClock(dateTime);
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));
            when(recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(TEAM_ID, SQUAD_ID))
                    .thenReturn(Optional.empty());
            when(recommendationCreator.createForSquad(any(), any()))
                    .thenReturn(createRecommendationWithCreatedAt(TEAM_ID, LocalDateTime.now()));
            when(memberRecommendationRepository.findByRecommendationId(any())).thenReturn(java.util.List.of());

            // when: 시간 검증 통과하여 정상 실행
            recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID);

            // then: Creator가 호출되었음을 검증 (시간 검증 통과)
            verify(recommendationCreator).createForSquad(any(), eq(RecommendationType.MANUAL));
        }
    }

    @Nested
    @DisplayName("미션 사이클 기반 중복 추천 검증")
    class MissionCycleValidation {

        @Test
        @DisplayName("현재 미션 사이클 내 추천이 있으면 스쿼드 수동 추천이 금지된다")
        void existingRecommendationInCycle_throwsException() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-15T06:30:00")
            );
            when(recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(TEAM_ID, SQUAD_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID))
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
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // 어제 추천 (이전 사이클)
            Recommendation oldRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-14T10:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(TEAM_ID, SQUAD_ID))
                    .thenReturn(Optional.of(oldRecommendation));
            when(recommendationCreator.createForSquad(any(), any()))
                    .thenReturn(createRecommendationWithCreatedAt(TEAM_ID, LocalDateTime.now()));
            when(memberRecommendationRepository.findByRecommendationId(any())).thenReturn(java.util.List.of());

            // when: 사이클 검증 통과하여 정상 실행
            recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID);

            // then
            verify(recommendationCreator).createForSquad(any(), eq(RecommendationType.MANUAL));
        }

        @Test
        @DisplayName("오전 6시 전에는 전날 06:00부터가 미션 사이클이다")
        void before6AM_usesYesterdayCycleStart() {
            // given: 새벽 3시 30분 (2025-01-15)
            Clock clock = fixedClock("2025-01-15T03:30:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // 2025-01-14 07:00에 생성된 추천 (현재 사이클)
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-14T07:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(TEAM_ID, SQUAD_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID))
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

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamId(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // 정확히 오늘 06:00:00에 생성된 추천
            Recommendation existingRecommendation = createRecommendationWithCreatedAt(
                    TEAM_ID, LocalDateTime.parse("2025-01-15T06:00:00")
            );
            when(recommendationRepository.findFirstByTeamIdAndSquadIdOrderByCreatedAtDesc(TEAM_ID, SQUAD_ID))
                    .thenReturn(Optional.of(existingRecommendation));

            // when & then
            assertThatThrownBy(() -> recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);
                    });
        }
    }

    // === Helper Methods ===

    private Team createTeamWithId(Long id) {
        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", id);
        return team;
    }

    private Squad createSquadWithId(Long squadId, Team team) {
        Squad squad = Squad.createDefault(team);
        setFieldValue(squad, "id", squadId);
        return squad;
    }

    private Recommendation createRecommendationWithCreatedAt(Long teamId, LocalDateTime createdAt) {
        Recommendation recommendation = Recommendation.createScheduledRecommendation(teamId);
        setFieldValue(recommendation, "createdAt", createdAt, true);
        return recommendation;
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
