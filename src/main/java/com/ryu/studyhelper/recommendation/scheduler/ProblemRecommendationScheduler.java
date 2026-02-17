package com.ryu.studyhelper.recommendation.scheduler;

import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
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

        notifyDiscord(result, failure, System.currentTimeMillis() - startTime);
    }

    private void notifyDiscord(BatchResult result, Exception failure, long elapsed) {
        try {
            if (failure != null) {
                discordNotifier.sendScheduler(DiscordMessage.error("문제 추천 배치 실패", failure, elapsed));
            } else {
                String title = result.failCount() > 0 ? "문제 추천 배치 실패" : "문제 추천 배치 완료";
                discordNotifier.sendScheduler(DiscordMessage.batchResult(
                        title, result.totalCount(), result.successCount(), result.failCount(), elapsed));
            }
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패", e);
        }
    }
}
