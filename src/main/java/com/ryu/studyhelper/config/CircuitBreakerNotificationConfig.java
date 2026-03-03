package com.ryu.studyhelper.config;

import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 서킷브레이커 상태 전이 이벤트 리스너 등록
 * CLOSED ↔ OPEN ↔ HALF_OPEN 전이 시 Discord 알림 발송
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerNotificationConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final DiscordNotifier discordNotifier;

    @PostConstruct
    public void registerListeners() {
        circuitBreakerRegistry.circuitBreaker("solvedAc")
                .getEventPublisher()
                .onStateTransition(event -> {
                    try {
                        String from = event.getStateTransition().getFromState().name();
                        String to   = event.getStateTransition().getToState().name();
                        discordNotifier.sendInfra(
                                DiscordMessage.circuitBreakerStateChange("solvedAc", from, to)
                        );
                    } catch (Exception e) {
                        log.warn("서킷브레이커 상태 전이 알림 전송 실패", e);
                    }
                });
    }
}
