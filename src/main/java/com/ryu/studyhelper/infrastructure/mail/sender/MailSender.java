package com.ryu.studyhelper.infrastructure.mail.sender;

/**
 * 메일 발송 인터페이스 (도메인 무관)
 */
public interface MailSender {
    void send(MailMessage message);
}