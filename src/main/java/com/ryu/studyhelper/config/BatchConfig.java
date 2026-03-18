package com.ryu.studyhelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 배치 작업용 스레드풀 설정
 * - recommendationBatchExecutor: 스쿼드별 SolvedAC 호출 병렬 처리
 */
@Configuration
public class BatchConfig {

    @Bean("recommendationBatchExecutor")
    public ThreadPoolTaskExecutor recommendationBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("recommendation-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
