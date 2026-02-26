package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.service.SquadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledRecommendationService 테스트")
class ScheduledRecommendationServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SquadRepository squadRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationCreator recommendationCreator;

    @Mock
    private SquadService squadService;

    private ScheduledRecommendationService scheduledRecommendationService;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Long TEAM_ID = 1L;

    private Clock fixedClock(String dateTime) {
        LocalDateTime ldt = LocalDateTime.parse(dateTime);
        Instant instant = ldt.atZone(ZONE_ID).toInstant();
        return Clock.fixed(instant, ZONE_ID);
    }

    private void setupServiceWithClock(Clock clock) {
        scheduledRecommendationService = new ScheduledRecommendationService(
                clock,
                teamRepository,
                squadRepository,
                recommendationRepository,
                recommendationCreator,
                squadService
        );
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

            Team team = createTeamWithIdAndRecommendationDay(TEAM_ID, DayOfWeek.WEDNESDAY);
            when(teamRepository.findAll()).thenReturn(List.of(team));

            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID), any(LocalDateTime.class), any(LocalDateTime.class)
            )).thenReturn(Optional.empty());

            // when
            scheduledRecommendationService.prepareDailyRecommendations();

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

            when(recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                    eq(TEAM_ID),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00")),
                    eq(LocalDateTime.parse("2025-01-15T06:00:00"))
            )).thenReturn(Optional.empty());

            // when
            scheduledRecommendationService.prepareDailyRecommendations();

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

            // when
            scheduledRecommendationService.prepareDailyRecommendations();

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

    // === Helper Methods ===

    private Team createTeamWithIdAndRecommendationDay(Long id, DayOfWeek dayOfWeek) {
        Team team = Team.create("테스트팀", "설명", false);
        try {
            java.lang.reflect.Field idField = team.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(team, id);

            com.ryu.studyhelper.team.domain.RecommendationDayOfWeek recommendationDay =
                    com.ryu.studyhelper.team.domain.RecommendationDayOfWeek.from(dayOfWeek);
            team.updateRecommendationDays(List.of(recommendationDay));

            java.lang.reflect.Field teamMembersField = team.getClass().getDeclaredField("teamMembers");
            teamMembersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> teamMembers = (java.util.List<Object>) teamMembersField.get(team);
            teamMembers.add(new Object());
        } catch (Exception e) {
            throw new RuntimeException("Team 설정 실패", e);
        }
        return team;
    }
}
