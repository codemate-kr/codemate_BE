package com.ryu.studyhelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS SES 클라이언트 설정
 *
 * 자격 증명 우선순위 (DefaultCredentialsProvider):
 * 1. 환경 변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 2. ~/.aws/credentials 파일
 * 3. EC2/ECS IAM 역할 (프로덕션 환경)
 */
@Configuration
@Profile("!test")
public class SesConfig {

    @Value("${aws.ses.region}")
    private String region;

    @Bean(destroyMethod = "close")
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}