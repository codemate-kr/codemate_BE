package com.ryu.studyhelper.recommendation.scheduler;

import com.ryu.studyhelper.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 오전 8시에 팀별 문제 추천을 자동 생성하는 스케줄러
 * Spring의 @Scheduled 어노테이션을 사용하여 Cron 표현식으로 실행 시점 정의
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRecommendationScheduler {

    private final RecommendationService recommendationService;

    /**
     * 매일 오전 8시에 모든 활성 팀에 대해 문제 추천 실행
     * Cron 표현식: "초 분 시 일 월 요일"
     * "0 0 8 * * *" = 매일 8시 0분 0초
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void executeDailyRecommendations() {
        log.info("=== 일일 문제 추천 배치 작업 시작 ===");
        
        long startTime = System.currentTimeMillis();
        
        try {
            recommendationService.createDailyRecommendationsForAllTeams();
            
            long endTime = System.currentTimeMillis();
            log.info("=== 일일 문제 추천 배치 작업 완료 === (소요시간: {}ms)", endTime - startTime);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("=== 일일 문제 추천 배치 작업 실패 === (소요시간: {}ms)", endTime - startTime, e);
            
            // TODO: 실패 시 알림 시스템 연동 (슬랙, 이메일 등)
            // notificationService.sendBatchFailureAlert("DailyRecommendation", e.getMessage());
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 테스트용: 매분 실행되는 스케줄러 (개발 시에만 사용)
     * 실제 배포 시에는 이 메서드를 주석 처리하거나 삭제해야 함
     */
    // @Scheduled(cron = "0 * * * * *") // 매분 실행
    public void testScheduler() {
        log.debug("테스트 스케줄러 실행: 매분 호출됨");
    }


    /**
     * 애플리케이션 시작 후 10초 뒤에 한 번 실행 (테스트용)
     * 실제 운영에서는 주석 처리 권장
     */
     @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void executeOnStartup() {
        log.info("애플리케이션 시작 후 테스트 추천 실행");
        try {
            recommendationService.createDailyRecommendationsForAllTeams();
        } catch (Exception e) {
            log.error("시작 시 추천 실행 실패", e);
        }
    }
}