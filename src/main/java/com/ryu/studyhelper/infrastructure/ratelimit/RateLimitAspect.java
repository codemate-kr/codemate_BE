package com.ryu.studyhelper.infrastructure.ratelimit;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Rate Limit AOP
 * @RateLimit 어노테이션이 붙은 메서드 실행 전 rate limit 체크
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RateLimiterService rateLimiterService;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 현재 인증된 사용자 ID 조회
        Long userId = getCurrentUserId();

        // 어노테이션에서 Rate Limit 타입 가져오기
        RateLimitType type = rateLimit.type();

        // Rate limit 체크
        if (!rateLimiterService.allowRequest(userId, type)) {
            long resetTime = rateLimiterService.getResetTimeInSeconds(userId, type);
            log.warn("Rate limit exceeded for user: {}, type: {}, reset in {} seconds",
                    userId, type, resetTime);
            throw new CustomException(CustomResponseStatus.RATE_LIMIT_EXCEEDED);
        }

        // Rate limit 통과 시 원래 메서드 실행
        return joinPoint.proceed();
    }

    /**
     * SecurityContext에서 현재 인증된 사용자 ID 조회
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PrincipalDetails principalDetails) {
            return principalDetails.getMemberId();
        }
        throw new CustomException(CustomResponseStatus.ACCESS_DENIED);
    }
}