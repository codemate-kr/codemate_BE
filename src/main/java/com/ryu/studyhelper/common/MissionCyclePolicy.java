package com.ryu.studyhelper.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 미션 사이클 도메인 정책
 * 매일 오전 6시를 기준으로 미션 사이클이 갱신된다.
 */
public class MissionCyclePolicy {

    public static final LocalTime MISSION_RESET_TIME = LocalTime.of(6, 0);

    /**
     * 현재 미션 사이클의 시작 시각을 반환한다.
     * 오전 6시 이전이면 전날 오전 6시를 반환한다.
     */
    public static LocalDateTime getMissionCycleStart(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return toMissionDate(now).atTime(MISSION_RESET_TIME);
    }

    /**
     * 주어진 시각이 속하는 미션 날짜를 반환한다.
     * 오전 6시 이전이면 전날로 취급한다.
     */
    public static LocalDate toMissionDate(LocalDateTime dateTime) {
        if (dateTime.toLocalTime().isBefore(MISSION_RESET_TIME)) {
            return dateTime.toLocalDate().minusDays(1);
        }
        return dateTime.toLocalDate();
    }
}