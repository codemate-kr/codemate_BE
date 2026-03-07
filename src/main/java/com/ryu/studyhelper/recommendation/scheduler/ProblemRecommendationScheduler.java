package com.ryu.studyhelper.recommendation.scheduler;

import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.service.RecommendationBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 문제 추천 스케줄러
 * - 06:00: 팀별 문제 추천 생성
 * - 07:00: FAILED 스쿼드 1회 재시도
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProblemRecommendationScheduler {

    private final RecommendationBatchService scheduledRecommendationService;
    private final DiscordNotifier discordNotifier;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void prepareDailyRecommendations() {
        log.info("=== 문제 추천 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();
        BatchResult result = null;
        Exception failure = null;

        try {
            result = scheduledRecommendationService.prepareDailyRecommendations();
            log.info("=== 문제 추천 배치 작업 완료 === (소요시간: {}ms)", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            failure = e;
            log.error("=== 문제 추천 배치 작업 실패 === (소요시간: {}ms)", System.currentTimeMillis() - startTime, e);
        }

        notifyMain(result, failure, System.currentTimeMillis() - startTime);
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void retryFailed() {
        log.info("=== 추천 재시도 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();
        BatchResult result = null;
        Exception failure = null;

        try {
            result = scheduledRecommendationService.retryFailed();
            log.info("=== 추천 재시도 배치 작업 완료 === (소요시간: {}ms)", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            failure = e;
            log.error("=== 추천 재시도 배치 작업 실패 === (소요시간: {}ms)", System.currentTimeMillis() - startTime, e);
        }

        notifyRetry(result, failure, System.currentTimeMillis() - startTime);
    }

    private void notifyMain(BatchResult result, Exception failure, long elapsed) {
        try {
            if (failure != null) {
                discordNotifier.sendScheduler(DiscordMessage.error("문제 추천 배치 실패", failure, elapsed));
            } else {
                String title = result.failCount() > 0 ? "문제 추천 배치 실패" : "문제 추천 배치 완료";
                discordNotifier.sendScheduler(DiscordMessage.batchResult(
                        title, result.totalCount(), result.successCount(), result.skipCount(), result.failCount(), elapsed));
            }
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패", e);
        }
    }

    private void notifyRetry(BatchResult result, Exception failure, long elapsed) {
        try {
            if (failure != null) {
                discordNotifier.sendScheduler(DiscordMessage.error("추천 재시도 배치 실패", failure, elapsed));
                return;
            }
            if (result.totalCount() == 0) {
                return;
            }
            String title = result.failCount() > 0 ? "추천 재시도 배치 완료 (실패 있음)" : "추천 재시도 배치 완료";
            discordNotifier.sendScheduler(DiscordMessage.batchResult(
                    title, result.totalCount(), result.successCount(), result.skipCount(), result.failCount(), elapsed));
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패", e);
        }
    }
}