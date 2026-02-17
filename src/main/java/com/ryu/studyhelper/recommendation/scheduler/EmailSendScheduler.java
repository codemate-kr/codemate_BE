package com.ryu.studyhelper.recommendation.scheduler;

import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.service.RecommendationEmailService;
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

    private final RecommendationEmailService recommendationEmailService;
    private final DiscordNotifier discordNotifier;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendPendingEmails() {
        log.info("=== 이메일 발송 배치 작업 시작 ===");

        long startTime = System.currentTimeMillis();
        BatchResult result = null;
        Exception failure = null;

        try {
            result = recommendationEmailService.sendAll();
            log.info("=== 이메일 발송 배치 작업 완료 === (소요시간: {}ms)", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            failure = e;
            log.error("=== 이메일 발송 배치 작업 실패 === (소요시간: {}ms)", System.currentTimeMillis() - startTime, e);
        }

        notifyDiscord(result, failure, System.currentTimeMillis() - startTime);
    }

    private void notifyDiscord(BatchResult result, Exception failure, long elapsed) {
        try {
            if (failure != null) {
                discordNotifier.sendScheduler(DiscordMessage.error("이메일 발송 배치 실패", failure, elapsed));
            } else {
                String title = result.failCount() > 0 ? "이메일 발송 배치 실패" : "이메일 발송 배치 완료";
                discordNotifier.sendScheduler(DiscordMessage.batchResult(
                        title, result.totalCount(), result.successCount(), result.failCount(), elapsed));
            }
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패", e);
        }
    }
}
