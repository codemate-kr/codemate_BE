package com.ryu.studyhelper.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Rate Limiter 서비스
 * Fixed Window Counter 알고리즘 사용
 * RateLimitType에 정의된 정책에 따라 동작
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Rate limit 체크 및 카운터 증가
     * @param userId 사용자 ID
     * @param type Rate Limit 타입
     * @return 요청 허용 여부 (true: 허용, false: 제한 초과)
     */
    public boolean allowRequest(Long userId, RateLimitType type) {
        String key = type.generateKey(userId);
        int maxRequests = type.getMaxRequests();
        long windowSeconds = type.getWindowSeconds();

        try {
            // 현재 카운터 값 조회
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = (currentCountStr != null) ? Integer.parseInt(currentCountStr) : 0;

            // Rate limit 초과 체크
            if (currentCount >= maxRequests) {
                log.warn("Rate limit exceeded for user: {}, type: {}, current count: {}",
                        userId, type, currentCount);
                return false;
            }

            // 카운터 증가
            Long newCount = redisTemplate.opsForValue().increment(key);

            // 첫 요청인 경우 TTL 설정
            if (newCount != null && newCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            log.debug("Rate limit check for user: {}, type: {}, count: {}/{}",
                    userId, type, newCount, maxRequests);
            return true;

        } catch (Exception e) {
            log.error("Failed to check rate limit for user: {}, type: {}", userId, type, e);
            // Redis 장애 시 요청 허용 (Fail-open 정책)
            return true;
        }
    }

    /**
     * 남은 요청 가능 횟수 조회
     * @param userId 사용자 ID
     * @param type Rate Limit 타입
     * @return 남은 요청 횟수
     */
    public int getRemainingRequests(Long userId, RateLimitType type) {
        String key = type.generateKey(userId);
        int maxRequests = type.getMaxRequests();

        try {
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = (currentCountStr != null) ? Integer.parseInt(currentCountStr) : 0;
            return Math.max(0, maxRequests - currentCount);
        } catch (Exception e) {
            log.error("Failed to get remaining requests for user: {}, type: {}", userId, type, e);
            return maxRequests;
        }
    }

    /**
     * Rate limit 초기화 (테스트 또는 관리자 용도)
     * @param userId 사용자 ID
     * @param type Rate Limit 타입
     */
    public void resetRateLimit(Long userId, RateLimitType type) {
        String key = type.generateKey(userId);
        redisTemplate.delete(key);
        log.info("Rate limit reset for user: {}, type: {}", userId, type);
    }

    /**
     * Rate limit 윈도우 남은 시간 조회 (초)
     * @param userId 사용자 ID
     * @param type Rate Limit 타입
     * @return 남은 시간(초), 없으면 0
     */
    public long getResetTimeInSeconds(Long userId, RateLimitType type) {
        String key = type.generateKey(userId);

        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return (ttl != null && ttl > 0) ? ttl : 0;
        } catch (Exception e) {
            log.error("Failed to get reset time for user: {}, type: {}", userId, type, e);
            return 0;
        }
    }
}