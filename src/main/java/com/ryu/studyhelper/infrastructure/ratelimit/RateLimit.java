package com.ryu.studyhelper.infrastructure.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limit을 적용할 메서드에 사용하는 어노테이션
 * type에 따라 서로 다른 제한 정책 적용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Rate Limit 타입 (기본값: SOLVED_AC)
     */
    RateLimitType type() default RateLimitType.SOLVED_AC;
}