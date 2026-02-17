package com.ryu.studyhelper.infrastructure.discord;

/**
 * Discord Webhook 알림 인터페이스
 * 채널별로 분리된 메서드 제공 (스케줄러, 이벤트)
 */
public interface DiscordNotifier {

    void sendScheduler(DiscordMessage message);

    void sendEvent(DiscordMessage message);
}
