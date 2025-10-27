package com.ryu.studyhelper.team.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Team 도메인 테스트")
class TeamTest {

    private Team team;

    @BeforeEach
    void setUp() {
        team = Team.create("테스트팀", "알고리즘 스터디");
    }

    @Test
    @DisplayName("팀 생성 시 추천 상태는 INACTIVE이고 요일은 비어있다")
    void create_initialState() {
        // when
        Team newTeam = Team.create("새 팀", "설명");

        // then
        assertThat(newTeam.isRecommendationActive()).isFalse();
        assertThat(newTeam.getRecommendationDaysList()).isEmpty();
    }

    @Test
    @DisplayName("updateRecommendationDays는 요일을 설정하고 추천 상태를 ACTIVE로 변경한다")
    void updateRecommendationDays_activatesRecommendation() {
        // given
        List<RecommendationDayOfWeek> days = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        // when
        team.updateRecommendationDays(days);

        // then
        assertThat(team.isRecommendationActive()).isTrue();
        assertThat(team.getRecommendationDaysList())
                .hasSize(3)
                .containsExactly(
                        RecommendationDayOfWeek.MONDAY,
                        RecommendationDayOfWeek.WEDNESDAY,
                        RecommendationDayOfWeek.FRIDAY
                );
    }

    @Test
    @DisplayName("updateRecommendationDays에 빈 List를 전달하면 추천 상태가 INACTIVE로 변경된다")
    void updateRecommendationDays_emptyList_deactivatesRecommendation() {
        // given: 먼저 요일을 설정
        team.updateRecommendationDays(Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.FRIDAY
        ));
        assertThat(team.isRecommendationActive()).isTrue();

        // when: 빈 List로 업데이트
        team.updateRecommendationDays(List.of());

        // then
        assertThat(team.isRecommendationActive()).isFalse();
        assertThat(team.getRecommendationDaysList()).isEmpty();
    }

    @Test
    @DisplayName("getRecommendationDaysList는 월요일부터 일요일까지 정렬된 순서로 반환한다")
    void getRecommendationDaysList_returnsOrderedList() {
        // given: 순서 없이 요일 설정 (금, 월, 수)
        List<RecommendationDayOfWeek> unorderedDays = Arrays.asList(
                RecommendationDayOfWeek.FRIDAY,
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY
        );
        team.updateRecommendationDays(unorderedDays);

        // when
        List<RecommendationDayOfWeek> retrievedDays = team.getRecommendationDaysList();

        // then: 월, 수, 금 순서로 정렬되어 반환
        assertThat(retrievedDays)
                .hasSize(3)
                .containsExactly(
                        RecommendationDayOfWeek.MONDAY,
                        RecommendationDayOfWeek.WEDNESDAY,
                        RecommendationDayOfWeek.FRIDAY
                );
    }

    @Test
    @DisplayName("isRecommendationDay는 설정된 요일에 대해 true를 반환한다")
    void isRecommendationDay_configuredDay_returnsTrue() {
        // given: 월, 수, 금 설정
        team.updateRecommendationDays(Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY,
                RecommendationDayOfWeek.FRIDAY
        ));

        // when & then
        assertThat(team.isRecommendationDay(DayOfWeek.MONDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.WEDNESDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.FRIDAY)).isTrue();
    }

    @Test
    @DisplayName("isRecommendationDay는 설정되지 않은 요일에 대해 false를 반환한다")
    void isRecommendationDay_notConfiguredDay_returnsFalse() {
        // given: 월, 수, 금 설정
        team.updateRecommendationDays(Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY,
                RecommendationDayOfWeek.FRIDAY
        ));

        // when & then
        assertThat(team.isRecommendationDay(DayOfWeek.TUESDAY)).isFalse();
        assertThat(team.isRecommendationDay(DayOfWeek.THURSDAY)).isFalse();
        assertThat(team.isRecommendationDay(DayOfWeek.SATURDAY)).isFalse();
        assertThat(team.isRecommendationDay(DayOfWeek.SUNDAY)).isFalse();
    }

    @Test
    @DisplayName("중복된 요일을 설정해도 올바르게 처리된다")
    void updateRecommendationDays_withDuplicates_handlesCorrectly() {
        // given: 월요일이 중복된 List
        List<RecommendationDayOfWeek> daysWithDuplicates = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        // when
        team.updateRecommendationDays(daysWithDuplicates);

        // then: 중복이 제거되고 월, 금만 저장됨
        List<RecommendationDayOfWeek> days = team.getRecommendationDaysList();
        assertThat(days)
                .hasSize(2)
                .containsExactly(
                        RecommendationDayOfWeek.MONDAY,
                        RecommendationDayOfWeek.FRIDAY
                );
    }

    @Test
    @DisplayName("모든 요일을 설정하면 매일 추천받을 수 있다")
    void updateRecommendationDays_allDays() {
        // given: 모든 요일
        List<RecommendationDayOfWeek> allDays = Arrays.asList(RecommendationDayOfWeek.values());

        // when
        team.updateRecommendationDays(allDays);

        // then
        assertThat(team.isRecommendationActive()).isTrue();
        assertThat(team.getRecommendationDaysList()).hasSize(7);
        assertThat(team.isRecommendationDay(DayOfWeek.MONDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.TUESDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.WEDNESDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.THURSDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.FRIDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.SATURDAY)).isTrue();
        assertThat(team.isRecommendationDay(DayOfWeek.SUNDAY)).isTrue();
    }

    @Test
    @DisplayName("난이도 프리셋을 설정하면 커스텀 레벨이 초기화된다")
    void updateProblemDifficultySettings_presetMode_clearsCustomLevels() {
        // given: 먼저 커스텀 설정
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.CUSTOM, 5, 15);
        assertThat(team.getMinProblemLevel()).isEqualTo(5);
        assertThat(team.getMaxProblemLevel()).isEqualTo(15);

        // when: 프리셋으로 변경
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.NORMAL, null, null);

        // then: 커스텀 값이 null로 초기화됨
        assertThat(team.getProblemDifficultyPreset()).isEqualTo(ProblemDifficultyPreset.NORMAL);
        assertThat(team.getMinProblemLevel()).isNull();
        assertThat(team.getMaxProblemLevel()).isNull();
    }

    @Test
    @DisplayName("커스텀 난이도를 설정하면 min/max 레벨이 저장된다")
    void updateProblemDifficultySettings_customMode_savesLevels() {
        // when
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.CUSTOM, 10, 20);

        // then
        assertThat(team.getProblemDifficultyPreset()).isEqualTo(ProblemDifficultyPreset.CUSTOM);
        assertThat(team.getMinProblemLevel()).isEqualTo(10);
        assertThat(team.getMaxProblemLevel()).isEqualTo(20);
    }

    @Test
    @DisplayName("getEffectiveMinProblemLevel은 프리셋 모드일 때 프리셋 값을 반환한다")
    void getEffectiveMinProblemLevel_presetMode() {
        // given
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.EASY, null, null);

        // when
        Integer effectiveMin = team.getEffectiveMinProblemLevel();

        // then
        assertThat(effectiveMin).isEqualTo(ProblemDifficultyPreset.EASY.getMinLevel());
    }

    @Test
    @DisplayName("getEffectiveMinProblemLevel은 커스텀 모드일 때 커스텀 값을 반환한다")
    void getEffectiveMinProblemLevel_customMode() {
        // given
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.CUSTOM, 7, 25);

        // when
        Integer effectiveMin = team.getEffectiveMinProblemLevel();

        // then
        assertThat(effectiveMin).isEqualTo(7);
    }

    @Test
    @DisplayName("getEffectiveMaxProblemLevel은 프리셋 모드일 때 프리셋 값을 반환한다")
    void getEffectiveMaxProblemLevel_presetMode() {
        // given
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.HARD, null, null);

        // when
        Integer effectiveMax = team.getEffectiveMaxProblemLevel();

        // then
        assertThat(effectiveMax).isEqualTo(ProblemDifficultyPreset.HARD.getMaxLevel());
    }

    @Test
    @DisplayName("getEffectiveMaxProblemLevel은 커스텀 모드일 때 커스텀 값을 반환한다")
    void getEffectiveMaxProblemLevel_customMode() {
        // given
        team.updateProblemDifficultySettings(ProblemDifficultyPreset.CUSTOM, 5, 18);

        // when
        Integer effectiveMax = team.getEffectiveMaxProblemLevel();

        // then
        assertThat(effectiveMax).isEqualTo(18);
    }
}