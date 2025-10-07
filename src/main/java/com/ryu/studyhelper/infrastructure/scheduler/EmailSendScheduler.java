package com.ryu.studyhelper.infrastructure.scheduler;

import com.ryu.studyhelper.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 오전 9시에 PENDING 상태 추천에 대해 이메일 발송하는 스케줄러
 * 새벽에 준비된 문제 추천을 사용자에게 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSendScheduler {

    private final RecommendationService recommendationService;

    /**
     * 매일 오전 9시에 이메일 발송
     * Cron 표현식: "0 0 9 * * *" = 매일 9시 0분 0초
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendPendingEmails() {
        log.info("=== 이메일 발송 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();

        try {
            recommendationService.sendPendingRecommendationEmails();

            long endTime = System.currentTimeMillis();
            log.info("=== 이메일 발송 배치 작업 완료 === (소요시간: {}ms)", endTime - startTime);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("=== 이메일 발송 배치 작업 실패 === (소요시간: {}ms)", endTime - startTime, e);
        }
    }
}