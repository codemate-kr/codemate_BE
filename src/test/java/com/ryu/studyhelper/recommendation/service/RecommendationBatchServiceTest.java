package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.concurrent.Executor;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationBatchService 테스트")
class RecommendationBatchServiceTest {

    @Mock
    private SquadRepository squadRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationSaver recommendationSaver;

    @Mock
    private RecommendationCreator recommendationCreator;

    @Mock
    private Executor recommendationBatchExecutor;

    private RecommendationBatchService scheduledRecommendationService;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Long TEAM_ID = 1L;
    private static final Long SQUAD_ID = 10L;

    private Clock fixedClock(String dateTime) {
        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateTime);
        Instant instant = ldt.atZone(ZONE_ID).toInstant();
        return Clock.fixed(instant, ZONE_ID);
    }

    private void setupServiceWithClock(Clock clock) {
        // 테스트에서 executor mock이 태스크를 동기적으로 실행하도록 설정 (미사용 stub 경고 방지를 위해 lenient)
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(recommendationBatchExecutor).execute(any(Runnable.class));

        scheduledRecommendationService = new RecommendationBatchService(
                clock,
                squadRepository,
                teamMemberRepository,
                recommendationRepository,
                recommendationSaver,
                recommendationCreator,
                recommendationBatchExecutor
        );
    }

    @Nested
    @DisplayName("정상 처리 흐름")
    class HappyPath {

        @Test
        @DisplayName("인증된 핸들이 있는 스쿼드는 PENDING 생성 후 처리된다")
        void squadWithHandles_createsPendingAndProcesses() {
            // given
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID, DayOfWeek.WEDNESDAY);
            when(squadRepository.findActiveSquadsForDay(anyInt())).thenReturn(List.of(squad));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad));
            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));

            Recommendation pending = createPending(SQUAD_ID, LocalDate.parse("2025-01-15"));
            when(recommendationSaver.createPending(eq(squad), any(LocalDate.class), eq(RecommendationType.SCHEDULED)))
                    .thenReturn(pending);

            // when
            BatchResult result = scheduledRecommendationService.prepareDailyRecommendations();

            // then
            verify(recommendationCreator).process(eq(pending), eq(squad));
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.skipCount()).isEqualTo(0);
            assertThat(result.failCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("스킵 처리")
    class SkipHandling {

        @Test
        @DisplayName("인증된 핸들이 없는 스쿼드는 PENDING 생성 없이 스킵된다")
        void squadWithNoHandles_skips() {
            // given
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID, DayOfWeek.WEDNESDAY);
            when(squadRepository.findActiveSquadsForDay(anyInt())).thenReturn(List.of(squad));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad));
            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of());

            // when
            BatchResult result = scheduledRecommendationService.prepareDailyRecommendations();

            // then
            verify(recommendationSaver, never()).createPending(any(), any(), any());
            verify(recommendationCreator, never()).process(any(), any());
            assertThat(result.skipCount()).isEqualTo(1);
            assertThat(result.successCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("UNIQUE 제약 위반 시 이미 선점된 스쿼드는 스킵된다")
        void duplicatePendingInsert_skips() {
            // given
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID, DayOfWeek.WEDNESDAY);
            when(squadRepository.findActiveSquadsForDay(anyInt())).thenReturn(List.of(squad));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad));
            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(recommendationSaver.createPending(any(), any(), any()))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

            // when
            BatchResult result = scheduledRecommendationService.prepareDailyRecommendations();

            // then
            verify(recommendationCreator, never()).process(any(), any());
            assertThat(result.successCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("실패 처리")
    class FailureHandling {

        @Test
        @DisplayName("API 처리 실패 시 failCount가 증가하고 다음 스쿼드는 계속 처리된다")
        void apiFailure_incrementsFailCount() {
            // given
            Clock clock = fixedClock("2025-01-15T06:00:00");
            setupServiceWithClock(clock);

            Squad squad1 = createSquadWithId(10L, TEAM_ID, DayOfWeek.WEDNESDAY);
            Squad squad2 = createSquadWithId(11L, TEAM_ID, DayOfWeek.WEDNESDAY);
            when(squadRepository.findActiveSquadsForDay(anyInt())).thenReturn(List.of(squad1, squad2));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad1, squad2));
            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(any(), any()))
                    .thenReturn(List.of("handle1"));

            Recommendation pending1 = createPending(10L, LocalDate.parse("2025-01-15"));
            Recommendation pending2 = createPending(11L, LocalDate.parse("2025-01-15"));
            when(recommendationSaver.createPending(eq(squad1), any(), any())).thenReturn(pending1);
            when(recommendationSaver.createPending(eq(squad2), any(), any())).thenReturn(pending2);

            org.mockito.Mockito.doThrow(new RuntimeException("API 오류"))
                    .when(recommendationCreator).process(eq(pending1), eq(squad1));

            // when
            BatchResult result = scheduledRecommendationService.prepareDailyRecommendations();

            // then
            verify(recommendationCreator).process(eq(pending2), eq(squad2));
            assertThat(result.failCount()).isEqualTo(1);
            assertThat(result.successCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("retryFailed")
    class RetryFailed {

        @Test
        @DisplayName("FAILED 레코드를 PENDING으로 전이 후 처리한다")
        void failedRecord_tryPrepareForRetryAndProcess() {
            // given
            Clock clock = fixedClock("2025-01-15T07:00:00");
            setupServiceWithClock(clock);

            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID, DayOfWeek.WEDNESDAY);
            Recommendation failedRec = createFailed(SQUAD_ID, LocalDate.parse("2025-01-15"));

            when(recommendationRepository.findByDateAndStatusIn(any(), eq(List.of(RecommendationStatus.FAILED))))
                    .thenReturn(List.of(failedRec));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad));
            when(recommendationSaver.tryPrepareForRetry(failedRec)).thenReturn(true);

            // when
            BatchResult result = scheduledRecommendationService.retryFailed();

            // then
            verify(recommendationSaver).tryPrepareForRetry(failedRec);
            verify(recommendationCreator).process(eq(failedRec), eq(squad));
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.failCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("점유 실패(다른 워커 선점) 시 process 미호출 + totalCount에서 제외")
        void alreadyClaimed_excludesFromTotal() {
            // given
            Clock clock = fixedClock("2025-01-15T07:00:00");
            setupServiceWithClock(clock);

            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID, DayOfWeek.WEDNESDAY);
            Recommendation failedRec = createFailed(SQUAD_ID, LocalDate.parse("2025-01-15"));

            when(recommendationRepository.findByDateAndStatusIn(any(), eq(List.of(RecommendationStatus.FAILED))))
                    .thenReturn(List.of(failedRec));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad));
            when(recommendationSaver.tryPrepareForRetry(failedRec)).thenReturn(false); // 다른 워커가 선점

            // when
            BatchResult result = scheduledRecommendationService.retryFailed();

            // then
            verify(recommendationCreator, never()).process(any(), any());
            assertThat(result.totalCount()).isEqualTo(0); // 선점된 항목은 이 워커의 대상에서 제외
            assertThat(result.skipCount()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("재시도 대상이 없으면 처리 없이 반환한다")
        void noTargets_returnsEmptyResult() {
            // given
            Clock clock = fixedClock("2025-01-15T07:00:00");
            setupServiceWithClock(clock);

            when(recommendationRepository.findByDateAndStatusIn(any(), any()))
                    .thenReturn(List.of());

            // when
            BatchResult result = scheduledRecommendationService.retryFailed();

            // then
            verify(recommendationSaver, never()).tryPrepareForRetry(any());
            verify(recommendationCreator, never()).process(any(), any());
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("스쿼드 정보가 없으면 해당 레코드는 스킵한다")
        void missingSquad_skips() {
            // given
            Clock clock = fixedClock("2025-01-15T07:00:00");
            setupServiceWithClock(clock);

            Recommendation failedRec = createFailed(SQUAD_ID, LocalDate.parse("2025-01-15"));

            when(recommendationRepository.findByDateAndStatusIn(any(), any()))
                    .thenReturn(List.of(failedRec));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of());

            // when
            BatchResult result = scheduledRecommendationService.retryFailed();

            // then
            verify(recommendationSaver, never()).tryPrepareForRetry(any());
            verify(recommendationCreator, never()).process(any(), any());
            assertThat(result.skipCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("재시도 중 예외 발생 시 failCount가 증가하고 다음 레코드는 계속 처리된다")
        void retryFailure_incrementsFailCount() {
            // given
            Clock clock = fixedClock("2025-01-15T07:00:00");
            setupServiceWithClock(clock);

            Squad squad1 = createSquadWithId(10L, TEAM_ID, DayOfWeek.WEDNESDAY);
            Squad squad2 = createSquadWithId(11L, TEAM_ID, DayOfWeek.WEDNESDAY);
            Recommendation failed1 = createFailed(10L, LocalDate.parse("2025-01-15"));
            Recommendation failed2 = createFailed(11L, LocalDate.parse("2025-01-15"));

            when(recommendationRepository.findByDateAndStatusIn(any(), any()))
                    .thenReturn(List.of(failed1, failed2));
            when(squadRepository.findByIdsWithTeam(any())).thenReturn(List.of(squad1, squad2));
            when(recommendationSaver.tryPrepareForRetry(any())).thenReturn(true);

            org.mockito.Mockito.doThrow(new RuntimeException("API 오류"))
                    .when(recommendationCreator).process(eq(failed1), eq(squad1));

            // when
            BatchResult result = scheduledRecommendationService.retryFailed();

            // then
            verify(recommendationCreator).process(eq(failed2), eq(squad2));
            assertThat(result.failCount()).isEqualTo(1);
            assertThat(result.successCount()).isEqualTo(1);
        }
    }

    // === Helper Methods ===

    private Squad createSquadWithId(Long squadId, Long teamId, DayOfWeek dayOfWeek) {
        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", teamId);

        Squad squad = Squad.createDefault(team);
        setFieldValue(squad, "id", squadId);

        RecommendationDayOfWeek recommendationDay = RecommendationDayOfWeek.from(dayOfWeek);
        squad.updateRecommendationDays(List.of(recommendationDay));

        return squad;
    }

    private Recommendation createPending(Long squadId, LocalDate date) {
        return Recommendation.createPending(TEAM_ID, squadId, RecommendationType.SCHEDULED, date);
    }

    private Recommendation createFailed(Long squadId, LocalDate date) {
        Recommendation rec = Recommendation.createPending(TEAM_ID, squadId, RecommendationType.SCHEDULED, date);
        rec.markAsFailed();
        return rec;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException(fieldName + " 설정 실패", ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 설정 실패", e);
        }
    }
}
