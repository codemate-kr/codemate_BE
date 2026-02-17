package com.ryu.studyhelper.infrastructure.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discord Webhook URL 설정
 * 채널별 Webhook URL 바인딩
 */
@ConfigurationProperties(prefix = "discord.webhooks")
public record DiscordProperties(
        String scheduler,
        String event
) {}
