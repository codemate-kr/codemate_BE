package com.ryu.studyhelper.infrastructure.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬/테스트용 Discord 알림 구현체
 * 실제 Webhook 호출 대신 로그 출력
 */
@Component
@Profile("!prod")
@Slf4j
public class FakeDiscordNotifier implements DiscordNotifier {

    @Override
    public void sendScheduler(DiscordMessage message) {
        log.info("[FAKE DISCORD] 스케줄러 알림: {}", extractTitle(message));
    }

    @Override
    public void sendEvent(DiscordMessage message) {
        log.info("[FAKE DISCORD] 이벤트 알림: {}", extractTitle(message));
    }

    private String extractTitle(DiscordMessage message) {
        if (message.embeds() == null || message.embeds().isEmpty()) {
            return "(제목 없음)";
        }
        return message.embeds().get(0).title();
    }
}
