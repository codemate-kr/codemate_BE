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
        log.debug("[FAKE DISCORD] 스케줄러 알림{}", formatMessage(message));
    }

    @Override
    public void sendEvent(DiscordMessage message) {
        log.debug("[FAKE DISCORD] 이벤트 알림{}", formatMessage(message));
    }

    @Override
    public void sendInfra(DiscordMessage message) {
        log.debug("[FAKE DISCORD] 인프라 알림{}", formatMessage(message));
    }

    private String formatMessage(DiscordMessage message) {
        if (message.embeds() == null || message.embeds().isEmpty()) {
            return " (내용 없음)";
        }
        DiscordMessage.Embed embed = message.embeds().get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("\n  title      : ").append(embed.title());
        if (embed.description() != null) {
            sb.append("\n  description: ").append(embed.description());
        }
        if (embed.fields() != null) {
            for (DiscordMessage.Field field : embed.fields()) {
                sb.append("\n  ").append(field.name()).append(": ").append(field.value());
            }
        }
        return sb.toString();
    }
}
