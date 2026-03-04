package com.ryu.studyhelper.infrastructure.mail.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 테스트용 MailSender
 * 실제 SES 호출 대신 지연 + 로그 출력
 */
@Component
@Profile("local")
@Primary
@Slf4j
public class FakeMailSender implements MailSender {

    private static final long DELAY_MS = 5_000; // 5초 지연

    @Override
    public void send(MailMessage message) {
        log.debug("[FAKE] 메일 발송 시작 - to: {}, subject: {} ({}ms 지연 시뮬레이션)",
                message.to(), message.subject(), DELAY_MS);
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("[FAKE] 메일 발송 완료 - to: {}", message.to());
    }
}