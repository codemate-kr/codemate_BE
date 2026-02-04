package com.ryu.studyhelper.team.mail;

import com.ryu.studyhelper.infrastructure.mail.sender.MailMessage;
import com.ryu.studyhelper.team.domain.TeamJoin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

/**
 * 팀 초대 메일 빌더
 */
@Component
@RequiredArgsConstructor
public class InvitationMailBuilder {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TemplateEngine templateEngine;

    @Value("${FRONTEND_URL:https://codemate.kr}")
    private String frontendUrl;

    /**
     * TeamJoin으로부터 메일 메시지 생성
     */
    public MailMessage build(TeamJoin teamJoin) {
        String to = teamJoin.getTargetMember().getEmail();
        String teamName = teamJoin.getTeam().getName();
        String subject = String.format("[CodeMate] '%s' 팀에 초대되었습니다", teamName);

        Context context = new Context();
        context.setVariable("teamName", teamName);
        context.setVariable("inviterHandle", teamJoin.getRequester().getHandle());
        context.setVariable("expiresAt", teamJoin.getExpiresAt().format(DATETIME_FORMATTER));
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

        String html = templateEngine.process("team-invitation-email", context);

        return new MailMessage(to, subject, html);
    }
}