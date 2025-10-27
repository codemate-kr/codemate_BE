package com.ryu.studyhelper.team.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationDayOfWeek 테스트")
class RecommendationDayOfWeekTest {

    @Test
    @DisplayName("fromBitMask는 월요일부터 일요일까지 순서대로 정렬된 List를 반환한다")
    void fromBitMask_returnsOrderedList() {
        // given: 금요일(16) + 월요일(1) + 수요일(4) = 21
        int bitMask = 21;

        // when
        List<RecommendationDayOfWeek> days = RecommendationDayOfWeek.fromBitMask(bitMask);

        // then: 월, 수, 금 순서로 정렬되어야 함
        assertThat(days)
                .hasSize(3)
                .containsExactly(
                        RecommendationDayOfWeek.MONDAY,
                        RecommendationDayOfWeek.WEDNESDAY,
                        RecommendationDayOfWeek.FRIDAY
                );
    }

    @Test
    @DisplayName("toBitMask는 순서에 관계없이 동일한 비트마스크 값을 반환한다")
    void toBitMask_orderDoesNotMatter() {
        // given
        List<RecommendationDayOfWeek> orderedList = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        List<RecommendationDayOfWeek> unorderedList = Arrays.asList(
                RecommendationDayOfWeek.FRIDAY,
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.WEDNESDAY
        );

        // when
        int orderedBitMask = RecommendationDayOfWeek.toBitMask(orderedList);
        int unorderedBitMask = RecommendationDayOfWeek.toBitMask(unorderedList);

        // then: 순서가 달라도 같은 비트마스크 값
        assertThat(orderedBitMask).isEqualTo(unorderedBitMask);
        assertThat(orderedBitMask).isEqualTo(21); // 1 + 4 + 16 = 21
    }

    @Test
    @DisplayName("toBitMask는 OR 연산을 사용하여 중복된 요일을 안전하게 처리한다")
    void toBitMask_handlesDuplicates() {
        // given: 월요일이 중복으로 들어간 List
        List<RecommendationDayOfWeek> daysWithDuplicates = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        List<RecommendationDayOfWeek> daysWithoutDuplicates = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        // when
        int bitMaskWithDuplicates = RecommendationDayOfWeek.toBitMask(daysWithDuplicates);
        int bitMaskWithoutDuplicates = RecommendationDayOfWeek.toBitMask(daysWithoutDuplicates);

        // then: OR 연산으로 중복이 무시되어 같은 값
        assertThat(bitMaskWithDuplicates).isEqualTo(bitMaskWithoutDuplicates);
        assertThat(bitMaskWithDuplicates).isEqualTo(17); // 1 | 16 = 17
    }

    @Test
    @DisplayName("toBitMask와 fromBitMask는 상호 변환이 가능하다")
    void toBitMask_and_fromBitMask_areReversible() {
        // given
        List<RecommendationDayOfWeek> originalDays = Arrays.asList(
                RecommendationDayOfWeek.TUESDAY,
                RecommendationDayOfWeek.THURSDAY,
                RecommendationDayOfWeek.SATURDAY
        );

        // when: List -> BitMask -> List
        int bitMask = RecommendationDayOfWeek.toBitMask(originalDays);
        List<RecommendationDayOfWeek> convertedDays = RecommendationDayOfWeek.fromBitMask(bitMask);

        // then: 원본과 동일한 요일들 (순서는 정렬됨)
        assertThat(convertedDays)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        RecommendationDayOfWeek.TUESDAY,
                        RecommendationDayOfWeek.THURSDAY,
                        RecommendationDayOfWeek.SATURDAY
                );
    }

    @Test
    @DisplayName("빈 List는 0(INACTIVE) 비트마스크를 반환한다")
    void toBitMask_emptyList_returnsInactive() {
        // given
        List<RecommendationDayOfWeek> emptyList = List.of();

        // when
        int bitMask = RecommendationDayOfWeek.toBitMask(emptyList);

        // then
        assertThat(bitMask).isEqualTo(RecommendationDayOfWeek.INACTIVE);
        assertThat(bitMask).isEqualTo(0);
    }

    @Test
    @DisplayName("INACTIVE(0) 비트마스크는 빈 List를 반환한다")
    void fromBitMask_inactive_returnsEmptyList() {
        // given
        int inactiveBitMask = RecommendationDayOfWeek.INACTIVE;

        // when
        List<RecommendationDayOfWeek> days = RecommendationDayOfWeek.fromBitMask(inactiveBitMask);

        // then
        assertThat(days).isEmpty();
    }

    @Test
    @DisplayName("모든 요일(월~일)을 비트마스크로 변환하면 EVERYDAY 값과 같다")
    void toBitMask_allDays_returnsEveryday() {
        // given: 모든 요일
        List<RecommendationDayOfWeek> allDays = Arrays.asList(RecommendationDayOfWeek.values());

        // when
        int bitMask = RecommendationDayOfWeek.toBitMask(allDays);

        // then
        assertThat(bitMask).isEqualTo(RecommendationDayOfWeek.EVERYDAY);
        assertThat(bitMask).isEqualTo(127); // 1+2+4+8+16+32+64
    }

    @Test
    @DisplayName("평일(월~금)만 비트마스크로 변환하면 WEEKDAYS 값과 같다")
    void toBitMask_weekdays_returnsWeekdays() {
        // given: 월~금
        List<RecommendationDayOfWeek> weekdays = Arrays.asList(
                RecommendationDayOfWeek.MONDAY,
                RecommendationDayOfWeek.TUESDAY,
                RecommendationDayOfWeek.WEDNESDAY,
                RecommendationDayOfWeek.THURSDAY,
                RecommendationDayOfWeek.FRIDAY
        );

        // when
        int bitMask = RecommendationDayOfWeek.toBitMask(weekdays);

        // then
        assertThat(bitMask).isEqualTo(RecommendationDayOfWeek.WEEKDAYS);
        assertThat(bitMask).isEqualTo(31); // 1+2+4+8+16
    }

    @Test
    @DisplayName("주말(토, 일)만 비트마스크로 변환하면 WEEKENDS 값과 같다")
    void toBitMask_weekends_returnsWeekends() {
        // given: 토, 일
        List<RecommendationDayOfWeek> weekends = Arrays.asList(
                RecommendationDayOfWeek.SATURDAY,
                RecommendationDayOfWeek.SUNDAY
        );

        // when
        int bitMask = RecommendationDayOfWeek.toBitMask(weekends);

        // then
        assertThat(bitMask).isEqualTo(RecommendationDayOfWeek.WEEKENDS);
        assertThat(bitMask).isEqualTo(96); // 32+64
    }

    @Test
    @DisplayName("isRecommendationDay는 해당 요일이 비트마스크에 포함되어 있으면 true를 반환한다")
    void isRecommendationDay_dayIncluded_returnsTrue() {
        // given: 월, 수, 금 (21)
        int bitMask = 21;

        // when & then
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.MONDAY)).isTrue();
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.WEDNESDAY)).isTrue();
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.FRIDAY)).isTrue();
    }

    @Test
    @DisplayName("isRecommendationDay는 해당 요일이 비트마스크에 포함되어 있지 않으면 false를 반환한다")
    void isRecommendationDay_dayNotIncluded_returnsFalse() {
        // given: 월, 수, 금 (21)
        int bitMask = 21;

        // when & then
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.TUESDAY)).isFalse();
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.THURSDAY)).isFalse();
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.SATURDAY)).isFalse();
        assertThat(RecommendationDayOfWeek.isRecommendationDay(bitMask, DayOfWeek.SUNDAY)).isFalse();
    }

    @Test
    @DisplayName("from 메서드는 DayOfWeek를 RecommendationDayOfWeek로 변환한다")
    void from_convertsJavaDayOfWeek() {
        // when & then
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.MONDAY))
                .isEqualTo(RecommendationDayOfWeek.MONDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.TUESDAY))
                .isEqualTo(RecommendationDayOfWeek.TUESDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.WEDNESDAY))
                .isEqualTo(RecommendationDayOfWeek.WEDNESDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.THURSDAY))
                .isEqualTo(RecommendationDayOfWeek.THURSDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.FRIDAY))
                .isEqualTo(RecommendationDayOfWeek.FRIDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.SATURDAY))
                .isEqualTo(RecommendationDayOfWeek.SATURDAY);
        assertThat(RecommendationDayOfWeek.from(DayOfWeek.SUNDAY))
                .isEqualTo(RecommendationDayOfWeek.SUNDAY);
    }
}