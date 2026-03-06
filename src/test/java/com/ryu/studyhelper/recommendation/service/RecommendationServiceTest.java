package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.dto.internal.CreationResult;
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
import java.time.LocalDate;
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
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

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
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));
            when(recommendationRepository.findByTeamIdAndSquadIdAndDate(eq(TEAM_ID), eq(SQUAD_ID), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(recommendationCreator.createForSquad(any(), any(), any()))
                    .thenReturn(Optional.of(new CreationResult(
                            createRecommendationWithDate(TEAM_ID, LocalDate.now(clock)), List.of(), List.of())));

            // when
            recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID);

            // then: Creator가 호출되었음을 검증 (시간 검증 통과)
            verify(recommendationCreator).createForSquad(any(), eq(RecommendationType.MANUAL), any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("미션 날짜 기반 중복 추천 검증")
    class MissionDateValidation {

        @Test
        @DisplayName("오늘 PENDING 추천이 있으면 스쿼드 수동 추천이 금지된다")
        void existingPendingInDate_throwsException() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            Recommendation existingRecommendation = createRecommendationWithDate(TEAM_ID, LocalDate.parse("2025-01-15"));
            when(recommendationRepository.findByTeamIdAndSquadIdAndDate(TEAM_ID, SQUAD_ID, LocalDate.parse("2025-01-15")))
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
        @DisplayName("오늘 FAILED 추천은 수동 추천을 허용한다")
        void existingFailedInDate_allowsNewRecommendation() {
            // given: 오전 10시
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // FAILED 상태 추천
            Recommendation failedRecommendation = createRecommendationWithDateAndStatus(
                    TEAM_ID, LocalDate.parse("2025-01-15"), RecommendationStatus.FAILED);
            when(recommendationRepository.findByTeamIdAndSquadIdAndDate(TEAM_ID, SQUAD_ID, LocalDate.parse("2025-01-15")))
                    .thenReturn(Optional.of(failedRecommendation));
            when(recommendationCreator.createForSquad(any(), any(), any()))
                    .thenReturn(Optional.of(new CreationResult(
                            createRecommendationWithDate(TEAM_ID, LocalDate.now(clock)), List.of(), List.of())));

            // when: 예외 없이 정상 실행
            recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID);

            // then
            verify(recommendationCreator).createForSquad(any(), eq(RecommendationType.MANUAL), any(LocalDate.class));
        }

        @Test
        @DisplayName("이전 날짜의 추천은 오늘 수동 추천에 영향을 주지 않는다")
        void previousDateRecommendation_allowsNewRecommendation() {
            // given: 오전 10시 (2025-01-15)
            Clock clock = fixedClock("2025-01-15T10:00:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // 오늘 날짜에 해당하는 레코드 없음
            when(recommendationRepository.findByTeamIdAndSquadIdAndDate(TEAM_ID, SQUAD_ID, LocalDate.parse("2025-01-15")))
                    .thenReturn(Optional.empty());
            when(recommendationCreator.createForSquad(any(), any(), any()))
                    .thenReturn(Optional.of(new CreationResult(
                            createRecommendationWithDate(TEAM_ID, LocalDate.now(clock)), List.of(), List.of())));

            // when
            recommendationService.createManualRecommendationForSquad(TEAM_ID, SQUAD_ID);

            // then
            verify(recommendationCreator).createForSquad(any(), eq(RecommendationType.MANUAL), any(LocalDate.class));
        }

        @Test
        @DisplayName("오전 6시 전에는 어제 날짜가 미션 날짜다")
        void before6AM_usesYesterdayMissionDate() {
            // given: 새벽 3시 30분 (2025-01-15) → 미션 날짜 = 2025-01-14
            Clock clock = fixedClock("2025-01-15T03:30:00");
            setupServiceWithClock(clock);

            Team team = createTeamWithId(TEAM_ID);
            Squad squad = createSquadWithId(SQUAD_ID, team);
            when(squadRepository.findByIdAndTeamIdWithTeam(SQUAD_ID, TEAM_ID)).thenReturn(Optional.of(squad));

            // 어제(2025-01-14) 날짜로 PENDING 추천 존재
            Recommendation existingRecommendation = createRecommendationWithDate(TEAM_ID, LocalDate.parse("2025-01-14"));
            when(recommendationRepository.findByTeamIdAndSquadIdAndDate(TEAM_ID, SQUAD_ID, LocalDate.parse("2025-01-14")))
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

    private Recommendation createRecommendationWithDate(Long teamId, LocalDate date) {
        return createRecommendationWithDateAndStatus(teamId, date, RecommendationStatus.PENDING);
    }

    private Recommendation createRecommendationWithDateAndStatus(Long teamId, LocalDate date, RecommendationStatus status) {
        Recommendation recommendation = Recommendation.createPending(teamId, SQUAD_ID, RecommendationType.SCHEDULED, date);
        if (status == RecommendationStatus.FAILED) {
            recommendation.markAsFailed();
        } else if (status == RecommendationStatus.SUCCESS) {
            recommendation.markAsSuccess();
        }
        return recommendation;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 설정 실패", e);
        }
    }
}
