package com.ryu.studyhelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS SES 클라이언트 설정
 *
 * 자격 증명 우선순위:
 * 1. Spring Environment (AWS_ACCESS_KEY_ID) - 로컬 .env 파일
 * 2. DefaultCredentialsProvider - EC2 IAM 역할 등
 */
@Configuration
@Profile("!test")
public class SesConfig {

    @Value("${aws.ses.region}")
    private String region;

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String secretAccessKey;

    @Bean(destroyMethod = "close")
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider())
                .build();
    }

    private AwsCredentialsProvider buildCredentialsProvider() {
        // Spring Environment에 키가 있으면 사용 (로컬 .env)
        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        // 없으면 DefaultCredentialsProvider 사용 (EC2 IAM 역할 등)
        return DefaultCredentialsProvider.builder().build();
    }
}