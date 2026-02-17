package com.ryu.studyhelper.infrastructure.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Discord Webhook 알림 구현체 (prod 전용)
 * RestClient로 Webhook POST 요청
 */
@Component
@Profile("prod")
@EnableConfigurationProperties(DiscordProperties.class)
@Slf4j
public class DiscordWebhookNotifier implements DiscordNotifier {

    private final DiscordProperties properties;
    private final RestClient restClient;

    public DiscordWebhookNotifier(DiscordProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public void sendScheduler(DiscordMessage message) {
        send(properties.scheduler(), message);
    }

    @Override
    public void sendEvent(DiscordMessage message) {
        send(properties.event(), message);
    }

    private void send(String webhookUrl, DiscordMessage message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord webhook URL이 설정되지 않아 알림을 건너뜁니다");
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패: {}", e.getMessage());
        }
    }
}
