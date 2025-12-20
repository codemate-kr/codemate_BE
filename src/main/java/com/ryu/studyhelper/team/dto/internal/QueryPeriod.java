package com.ryu.studyhelper.team.dto.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 조회 기간을 나타내는 내부 DTO
 * TeamActivityService 내부에서만 사용됩니다.
 *
 * 미션 사이클: 오전 6시 기준
 * - 12월 20일 미션: 12월 20일 06:00 ~ 12월 21일 05:59:59
 */
public record QueryPeriod(
        int days,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
    private static final LocalTime MISSION_RESET_TIME = LocalTime.of(6, 0);

    public static QueryPeriod of(int days, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atTime(MISSION_RESET_TIME);
        LocalDateTime endDateTime = endDate.plusDays(1).atTime(MISSION_RESET_TIME);
        return new QueryPeriod(days, startDate, endDate, startDateTime, endDateTime);
    }
}