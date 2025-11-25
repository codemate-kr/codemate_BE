package com.ryu.studyhelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 시간 관련 설정
 * 단위 테스트에서 Clock을 모킹하여 시간 의존적 로직을 테스트할 수 있습니다.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}