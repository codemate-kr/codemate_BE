package com.ryu.studyhelper.recommendation.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 미션 사이클 도메인 정책
 * 매일 오전 6시를 기준으로 미션 사이클이 갱신된다.
 */
class MissionCyclePolicy {

    static final LocalTime MISSION_RESET_TIME = LocalTime.of(6, 0);

    static LocalDateTime getMissionCycleStart(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.toLocalTime().isBefore(MISSION_RESET_TIME)) {
            return now.toLocalDate().minusDays(1).atTime(MISSION_RESET_TIME);
        }
        return now.toLocalDate().atTime(MISSION_RESET_TIME);
    }
}
