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
@Profile("!prod")
@Slf4j
public class FakeMailSender implements MailSender {

    private static final long DELAY_MS = 5_000; // 5초 지연

    @Override
    public void send(MailMessage message) {
        log.debug("[FAKE] 메일 발송 시작 ({}ms 지연 시뮬레이션)\n  to     : {}\n  subject: {}\n  body   : {}자",
                DELAY_MS, message.to(), message.subject(),
                message.html() != null ? message.html().length() : 0);
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("[FAKE] 메일 발송 완료 - to: {}, subject: {}", message.to(), message.subject());
    }
}