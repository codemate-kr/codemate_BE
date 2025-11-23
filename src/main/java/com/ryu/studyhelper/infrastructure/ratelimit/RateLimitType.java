package com.ryu.studyhelper.infrastructure.ratelimit;

import lombok.Getter;

/**
 * Rate Limit 정책 타입
 * 각 외부 API별 제한 정책 정의
 */
@Getter
public enum RateLimitType {
    /**
     * solved.ac API
     * - solved.ac는 256/15분 제한이 있음
     * - 안전하게 분당 10회로 제한 (15분에 150회 = 256회보다 안전)
     */
    SOLVED_AC(10, 60, "rate_limit:solvedac:");

    private final int maxRequests;      // 최대 요청 횟수
    private final long windowSeconds;   // 시간 윈도우 (초)
    private final String keyPrefix;     // Redis 키 prefix

    RateLimitType(int maxRequests, long windowSeconds, String keyPrefix) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 사용자별 Redis 키 생성
     */
    public String generateKey(Long userId) {
        return keyPrefix + userId;
    }
}