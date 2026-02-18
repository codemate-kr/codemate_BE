package com.ryu.studyhelper.member.mail;

import com.ryu.studyhelper.infrastructure.mail.sender.MailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 이메일 변경 인증 메일 생성
 */
@Component
@RequiredArgsConstructor
public class EmailChangeMailBuilder {

    private final TemplateEngine templateEngine;
    private final EmailVerificationTokenProvider tokenProvider;

    @Value("${FRONTEND_URL:https://codemate.kr}")
    private String frontendUrl;

    /**
     * 이메일 변경 인증 메일 생성
     * @param memberId 회원 ID
     * @param newEmail 변경할 이메일
     */
    public MailMessage build(Long memberId, String newEmail) {
        String token = tokenProvider.createToken(memberId, newEmail);
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

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