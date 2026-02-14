package com.ryu.studyhelper.recommendation.scheduler;

import com.ryu.studyhelper.recommendation.service.ScheduledRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 오전 6시에 팀별 문제 추천을 준비하는 스케줄러
 * solved.ac API 호출 및 DB 저장만 수행 (이메일 발송 X)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProblemRecommendationScheduler {

    private final ScheduledRecommendationService scheduledRecommendationService;

    /**
     * 매일 오전 6시에 문제 추천 준비
     * Cron 표현식: "0 0 6 * * *" = 매일 6시 0분 0초
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void prepareDailyRecommendations() {
        log.info("=== 문제 추천 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();

        try {
            scheduledRecommendationService.prepareDailyRecommendations();

            long endTime = System.currentTimeMillis();
            log.info("=== 문제 추천 배치 작업 완료 === (소요시간: {}ms)", endTime - startTime);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("=== 문제 추천 배치 작업 실패 === (소요시간: {}ms)", endTime - startTime, e);
        }
    }

    // 테스트용 첫 실행후 1초뒤에 딱 한번 문제 추천 배치 작업 시작
//    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void testPrepareRecommendations() {
        log.info("=== [테스트] 문제 추천 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();

        try {
            scheduledRecommendationService.prepareDailyRecommendations();

            long endTime = System.currentTimeMillis();
            log.info("=== [테스트] 문제 추천 배치 작업 완료 === (소요시간: {}ms)", endTime - startTime);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("=== [테스트] 문제 추천 배치 작업 실패 === (소요시간: {}ms)", endTime - startTime, e);
        }
    }
}
