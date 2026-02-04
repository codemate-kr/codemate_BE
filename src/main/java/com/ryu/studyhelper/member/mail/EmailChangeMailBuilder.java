package com.ryu.studyhelper.member.mail;

import com.ryu.studyhelper.infrastructure.mail.sender.MailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 이메일 변경 인증 메일 빌더
 */
@Component
@RequiredArgsConstructor
public class EmailChangeMailBuilder {

    private final TemplateEngine templateEngine;

    /**
     * 이메일 변경 인증 메일 생성
     */
    public MailMessage build(String newEmail, String verificationUrl) {
        if (newEmail == null || newEmail.isBlank()) {
            throw new IllegalArgumentException("newEmail은 필수입니다");
        }
        if (verificationUrl == null || verificationUrl.isBlank()) {
            throw new IllegalArgumentException("verificationUrl은 필수입니다");
        }
        String subject = "[CodeMate] 이메일 변경 인증";

        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("message", "아래 버튼을 클릭하여 이메일 변경을 완료해주세요. 링크는 5분간 유효합니다.");
        context.setVariable("buttonUrl", verificationUrl);
        context.setVariable("buttonText", "이메일 인증하기");

        String html = templateEngine.process("email-change-template", context);

        return new MailMessage(newEmail, subject, html);
    }
}