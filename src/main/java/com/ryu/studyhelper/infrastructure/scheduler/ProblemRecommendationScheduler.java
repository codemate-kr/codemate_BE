package com.ryu.studyhelper.infrastructure.scheduler;

import com.ryu.studyhelper.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 새벽 2시에 팀별 문제 추천을 준비하는 스케줄러
 * solved.ac API 호출 및 DB 저장만 수행 (이메일 발송 X)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProblemRecommendationScheduler {

    private final RecommendationService recommendationService;

    /**
     * 매일 새벽 2시에 문제 추천 준비
     * Cron 표현식: "0 0 2 * * *" = 매일 2시 0분 0초
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void prepareDailyRecommendations() {
        log.info("=== 문제 추천 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();

        try {
            recommendationService.prepareDailyRecommendations();

            long endTime = System.currentTimeMillis();
            log.info("=== 문제 추천 배치 작업 완료 === (소요시간: {}ms)", endTime - startTime);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("=== 문제 추천 배치 작업 실패 === (소요시간: {}ms)", endTime - startTime, e);
        }
    }
}