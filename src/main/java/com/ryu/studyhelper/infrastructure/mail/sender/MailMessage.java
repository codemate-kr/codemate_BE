package com.ryu.studyhelper.infrastructure.mail.sender;

/**
 * 메일 발송용 공통 DTO
 */
public record MailMessage(
        String to,
        String subject,
        String html
) {}