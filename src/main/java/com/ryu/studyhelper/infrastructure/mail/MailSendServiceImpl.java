package com.ryu.studyhelper.infrastructure.mail;

import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.MailTxtSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.ProblemView;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * AWS SES 기반 이메일 발송 서비스
 */
@Service
@Slf4j
public class MailSendServiceImpl implements MailSendService {
    private static final String SENDER_NAME = "CodeMate";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final CssInlinerService cssInlinerService;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.configuration-set:#{null}}")
    private String configurationSetName;

    @Value("${FRONTEND_URL:https://codemate.kr}")
    private String frontendUrl;

    public MailSendServiceImpl(SesClient sesClient, TemplateEngine templateEngine, CssInlinerService cssInlinerService) {
        this.sesClient = sesClient;
        this.templateEngine = templateEngine;
        this.cssInlinerService = cssInlinerService;
    }

    @Override
    public void sendTxtEmail(MailTxtSendDto dto) {
        sendEmail(dto.getEmailAddr(), dto.getSubject(), null, dto.getContent());
    }

    @Override
    public void sendHtmlEmail(MailHtmlSendDto dto) {
        Context context = new Context();
        context.setVariable("subject", dto.getSubject());
        context.setVariable("message", dto.getContent());
        context.setVariable("buttonUrl", dto.getButtonUrl());
        context.setVariable("buttonText", dto.getButtonText());

        String templateName = dto.getTemplateName() == null || dto.getTemplateName().isBlank()
                ? "email-template"
                : dto.getTemplateName();
        String htmlContent = templateEngine.process(templateName, context);

        sendEmail(dto.getEmailAddr(), dto.getSubject(), htmlContent, null);
    }

    @Override
    public void sendMemberRecommendationEmail(MemberRecommendation memberRecommendation) {
        String memberEmail = memberRecommendation.getMember().getEmail();
        if (memberEmail == null || memberEmail.isBlank()) {
            throw new IllegalArgumentException("회원 이메일이 없습니다");
        }

        String subject = buildMemberRecommendationSubject(memberRecommendation);
        String htmlContent = buildMemberRecommendationHtml(memberRecommendation, subject);
        String plainText = buildMemberRecommendationPlainText(memberRecommendation);

        sendEmail(memberEmail, subject, htmlContent, plainText);
    }

    // ==================== 공통 발송 로직 ====================

    /**
     * 이메일 발송 공통 메서드
     * @param to 수신자 이메일
     * @param subject 제목
     * @param htmlBody HTML 본문 (null이면 텍스트만 발송)
     * @param textBody 텍스트 본문 (null이면 HTML만 발송)
     */
    private void sendEmail(String to, String subject, String htmlBody, String textBody) {
        try {
            Body.Builder bodyBuilder = Body.builder();
            if (htmlBody != null) {
                bodyBuilder.html(createContent(htmlBody));
            }
            if (textBody != null) {
                bodyBuilder.text(createContent(textBody));
            }

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(SENDER_NAME + " <" + fromEmail + ">")
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(createContent(subject))
                            .body(bodyBuilder.build())
                            .build());

            if (configurationSetName != null && !configurationSetName.isBlank()) {
                requestBuilder.configurationSetName(configurationSetName);
            }

            sesClient.sendEmail(requestBuilder.build());
            log.debug("이메일 발송 완료: {}", to);
        } catch (SesException e) {
            log.error("이메일 전송 실패 ({}): {}", to, e.getMessage());
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    private Content createContent(String data) {
        return Content.builder()
                .data(data)
                .charset(StandardCharsets.UTF_8.name())
                .build();
    }

    // ==================== 추천 이메일 빌더 ====================

    private String buildMemberRecommendationSubject(MemberRecommendation memberRecommendation) {
        return String.format("[CodeMate] 오늘의 미션 문제 (%s)",
                memberRecommendation.getRecommendation().getCreatedAt().format(DATE_FORMATTER));
    }

    private String buildMemberRecommendationHtml(MemberRecommendation memberRecommendation, String subject) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("recommendationDate",
                memberRecommendation.getRecommendation().getCreatedAt().format(DATE_FORMATTER));
        context.setVariable("teamPageUrl", buildTeamPageUrl(memberRecommendation.getTeamId()));

        List<ProblemView> problems = memberRecommendation.getRecommendation().getProblems().stream()
                .map(rp -> toProblemView(rp, memberRecommendation.getRecommendation().getProblems().indexOf(rp) + 1))
                .toList();
        context.setVariable("problems", problems);

        try {
            context.setVariable("logoImage", getBase64EncodedImage("static/images/logo.png"));
        } catch (IOException e) {
            context.setVariable("logoImage", null);
        }

        String htmlContent = templateEngine.process("recommendation-email-v3", context);
        return cssInlinerService.inlineCss(htmlContent, "static/css/email-recommendation-v2.css");
    }

    private String buildMemberRecommendationPlainText(MemberRecommendation memberRecommendation) {
        String teamName = memberRecommendation.getTeamName() != null
                ? memberRecommendation.getTeamName()
                : "팀";

        StringBuilder content = new StringBuilder();
        content.append(String.format("안녕하세요! %s팀의 오늘 추천 문제입니다.\n\n", teamName));

        List<RecommendationProblem> problems = memberRecommendation.getRecommendation().getProblems();
        for (int i = 0; i < problems.size(); i++) {
            Problem problem = problems.get(i).getProblem();
            content.append(String.format("%d. %s\n", i + 1, problem.getTitleKo()));
            content.append(String.format("   레벨: %d | URL: %s\n\n", problem.getLevel(), problem.getUrl()));
        }

        content.append("오늘도 화이팅하세요!\n");
        return content.toString();
    }

    // ==================== 유틸리티 ====================

    private String buildTeamPageUrl(Long teamId) {
        if (teamId == null) {
            throw new IllegalArgumentException("팀 ID는 null일 수 없습니다");
        }
        return frontendUrl + "/teams/" + teamId;
    }

    private ProblemView toProblemView(RecommendationProblem rp, int order) {
        Problem p = rp.getProblem();
        return new ProblemView(
                order,
                p.getTitleKo(),
                p.getLevel(),
                p.getUrl(),
                p.getId(),
                p.getAcceptedUserCount(),
                p.getAverageTries(),
                false
        );
    }

    private String getBase64EncodedImage(String imagePath) throws IOException {
        Resource resource = new ClassPathResource(imagePath);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return Base64.getEncoder().encodeToString(bytes);
    }
}