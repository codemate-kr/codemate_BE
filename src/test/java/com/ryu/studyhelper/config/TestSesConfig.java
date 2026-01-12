package com.ryu.studyhelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.ses.SesClient;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경용 SesClient Mock 설정
 * 실제 AWS 연결 없이 테스트 수행
 */
@Configuration
@Profile("test")
public class TestSesConfig {

    @Bean
    @Primary
    public SesClient sesClient() {
        return mock(SesClient.class);
    }
}