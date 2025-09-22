package com.ryu.studyhelper.team.domain;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 팀별 문제 추천 요일 설정을 위한 열거형
 * 여러 요일을 조합하여 저장할 수 있도록 비트마스크 방식 사용
 */
public enum RecommendationDayOfWeek {
    
    MONDAY(1, DayOfWeek.MONDAY, "월요일"),
    TUESDAY(2, DayOfWeek.TUESDAY, "화요일"), 
    WEDNESDAY(4, DayOfWeek.WEDNESDAY, "수요일"),
    THURSDAY(8, DayOfWeek.THURSDAY, "목요일"),
    FRIDAY(16, DayOfWeek.FRIDAY, "금요일"),
    SATURDAY(32, DayOfWeek.SATURDAY, "토요일"),
    SUNDAY(64, DayOfWeek.SUNDAY, "일요일");

    private final int bitValue;
    private final DayOfWeek dayOfWeek;
    private final String koreanName;

    RecommendationDayOfWeek(int bitValue, DayOfWeek dayOfWeek, String koreanName) {
        this.bitValue = bitValue;
        this.dayOfWeek = dayOfWeek;
        this.koreanName = koreanName;
    }

    public int getBitValue() {
        return bitValue;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 여러 요일을 비트마스크 값으로 변환
     */
    public static int toBitMask(Set<RecommendationDayOfWeek> days) {
        return days.stream()
                .mapToInt(RecommendationDayOfWeek::getBitValue)
                .sum();
    }

    /**
     * 비트마스크 값을 요일 Set으로 변환
     */
    public static Set<RecommendationDayOfWeek> fromBitMask(int bitMask) {
        return Arrays.stream(values())
                .filter(day -> (bitMask & day.getBitValue()) != 0)
                .collect(Collectors.toSet());
    }

    /**
     * 현재 요일이 설정된 요일에 포함되는지 확인
     */
    public static boolean isRecommendationDay(int bitMask, DayOfWeek currentDay) {
        return Arrays.stream(values())
                .filter(day -> day.getDayOfWeek() == currentDay)
                .anyMatch(day -> (bitMask & day.getBitValue()) != 0);
    }

    /**
     * DayOfWeek를 RecommendationDayOfWeek로 변환
     */
    public static RecommendationDayOfWeek from(DayOfWeek dayOfWeek) {
        return Arrays.stream(values())
                .filter(day -> day.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown DayOfWeek: " + dayOfWeek));
    }

    /**
     * 평일(월~금) 기본 설정
     */
    public static final int WEEKDAYS = MONDAY.bitValue | TUESDAY.bitValue | WEDNESDAY.bitValue 
                                     | THURSDAY.bitValue | FRIDAY.bitValue;

    /**
     * 주말(토, 일) 기본 설정  
     */
    public static final int WEEKENDS = SATURDAY.bitValue | SUNDAY.bitValue;

    /**
     * 매일 설정
     */
    public static final int EVERYDAY = WEEKDAYS | WEEKENDS;

    /**
     * 비활성화 (추천 받지 않음)
     */
    public static final int INACTIVE = 0;
}