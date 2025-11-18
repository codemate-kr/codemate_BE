package com.ryu.studyhelper.config;

import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import io.sentry.SentryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Sentry 에러 추적 설정
 *
 * 참고:
 * - sentry-spring-boot-starter-jakarta가 자동 설정 제공
 * - application.yml의 sentry.* 설정으로 자동 초기화됨
 * - CustomException 중 4xx는 BeforeSend 콜백에서 필터링
 */
@Slf4j
@Configuration
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String dsn;

    @Value("${sentry.environment:local}")
    private String environment;

    @PostConstruct
    public void init() {
        if (dsn == null || dsn.isBlank()) {
            log.warn("Sentry DSN이 설정되지 않았습니다. Sentry가 비활성화됩니다.");
            return;
        }

        log.info("Sentry 초기화 완료 - 환경: {}", environment);
        log.info("Sentry DSN: {}...", dsn.substring(0, Math.min(30, dsn.length())));
    }

    /**
     * Sentry BeforeSend 콜백 설정
     * - CustomException 중 5xx 에러만 Sentry로 전송
     * - 4xx 이하는 전송 차단
     */
    @Bean
    @ConditionalOnProperty(name = "sentry.dsn")
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            Throwable throwable = event.getThrowable();

            if (throwable instanceof CustomException customEx) {
                CustomResponseStatus status = customEx.getStatus();
                int httpStatus = status.getHttpStatusCode();

                // 500 미만이면 Sentry로 보내지 않고 드롭
                if (httpStatus < 500) {
                    return null; // null 반환 = 이벤트 무시
                }

            }

            // 그 외(5xx 커스텀예외 + 다른 예외)는 그대로 전송
            return event;
        };
    }

}