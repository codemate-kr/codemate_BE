package com.ryu.studyhelper.infrastructure.mail;

import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.MailTxtSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.ProblemView;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendationProblem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;                 // <-- 여기!
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;                           // <-- 여기!
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class MailSendServiceImpl implements MailSendService {
    private static final String SENDER_NAME = "CodeMate";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final CssInlinerService cssInlinerService;

    @Value("${spring.mail.username}")
    private String emailSender;

    @Value("${FRONTEND_URL:https://codemate.kr}")
    private String frontendUrl;

    public MailSendServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine, CssInlinerService cssInlinerService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.cssInlinerService = cssInlinerService;
    }

    /**
     * 발신자 이메일 주소 포맷팅
     */
    private String getFromAddress() {
        return SENDER_NAME + " <" + emailSender + ">";
    }

    /**
     * MimeMessageHelper 생성 (multipart 지원)
     */
    private MimeMessageHelper createMimeMessageHelper() throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        return new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
    }

    /**
     * 텍스트 기반 메일 전송
     */
    @Override
    public void sendTxtEmail(MailTxtSendDto mailTxtSendDto) {
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setTo(mailTxtSendDto.getEmailAddr());
        smm.setFrom(getFromAddress());
        smm.setSubject(mailTxtSendDto.getSubject());
        smm.setText(mailTxtSendDto.getContent());

        try {
            mailSender.send(smm);
        } catch (MailException e) {
            throw new RuntimeException("텍스트 메일 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * HTML 템플릿 기반 메일 전송
     */
    @Override
    public void sendHtmlEmail(MailHtmlSendDto mailHtmlSendDto) {
        try {
            MimeMessageHelper helper = createMimeMessageHelper();

            // Thymeleaf 템플릿 컨텍스트 구성
            Context context = new Context();
            context.setVariable("subject", mailHtmlSendDto.getSubject());
            context.setVariable("message", mailHtmlSendDto.getContent());
            context.setVariable("buttonUrl", mailHtmlSendDto.getButtonUrl());
            context.setVariable("buttonText", mailHtmlSendDto.getButtonText());

            // 템플릿 이름 지정 (기본 email-template)
            String templateName = mailHtmlSendDto.getTemplateName() == null || mailHtmlSendDto.getTemplateName().isBlank()
                    ? "email-template"
                    : mailHtmlSendDto.getTemplateName();
            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(getFromAddress());
            helper.setTo(mailHtmlSendDto.getEmailAddr());
            helper.setSubject(mailHtmlSendDto.getSubject());
            helper.setText(htmlContent, true);

            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            throw new RuntimeException("HTML 메일 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 팀 추천 이메일 발송
     */
    @Override
    public void sendRecommendationEmail(TeamRecommendation recommendation, List<String> memberEmails) {
        String subject = buildRecommendationSubject(recommendation);
        String htmlContent = buildRecommendationHtml(recommendation, subject);
        String plainText = buildRecommendationContent(recommendation);

        try {
            for (String email : memberEmails) {
                MimeMessageHelper helper = createMimeMessageHelper();
                helper.setFrom(getFromAddress());
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(plainText, htmlContent);
                mailSender.send(helper.getMimeMessage());
            }
        } catch (MessagingException e) {
            throw new RuntimeException("추천 HTML 메일 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 추천 이메일 HTML 내용 생성
     */
    private String buildRecommendationHtml(TeamRecommendation recommendation, String subject) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("recommendationDate", recommendation.getRecommendationDate().format(DATE_FORMATTER));

        // Prepare problem view models for template
        List<ProblemView> problems = recommendation.getProblems().stream()
                .map(trp -> {
                    Problem p = trp.getProblem();
                    return new ProblemView(
                            trp.getRecommendationOrder(),
                            p.getTitleKo(),
                            p.getLevel(),
                            p.getUrl(),
                            p.getId(),
                            p.getAcceptedUserCount(),
                            p.getAverageTries(),
                            false  // TODO: 완료 여부 확인 로직 필요
                    );
                })
                .toList();
        context.setVariable("problems", problems);

        // 로고 이미지 추가
        try {
            String base64Image = getBase64EncodedImage("static/images/logo.png");
            context.setVariable("logoImage", base64Image);
        } catch (IOException e) {
            context.setVariable("logoImage", null);
        }

        String htmlContent = templateEngine.process("recommendation-email-v2", context);
        return cssInlinerService.inlineCss(htmlContent, "static/css/email-recommendation-v2.css");
    }

    /**
     * 추천 이메일 제목 생성
     */
    private String buildRecommendationSubject(TeamRecommendation recommendation) {
        return String.format("[CodeMate] 오늘의 미션 문제 (%s)",
                recommendation.getRecommendationDate().format(DATE_FORMATTER)
        );
    }

    /**
     * 추천 이메일 내용 생성
     */
    private String buildRecommendationContent(TeamRecommendation recommendation) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("안녕하세요! %s팀의 오늘 추천 문제입니다.\n\n", 
                recommendation.getTeam().getName()));

        for (TeamRecommendationProblem trp : recommendation.getProblems()) {
            Problem problem = trp.getProblem();
            content.append(String.format("%d. %s\n", 
                    trp.getRecommendationOrder(), problem.getTitleKo()));
            content.append(String.format("   레벨: %d | URL: %s\n\n", 
                    problem.getLevel(), problem.getUrl()));
        }

        content.append("오늘도 화이팅하세요! 💪\n");
        return content.toString();
    }

    /**
     * 개인 추천 이메일 발송 (신규 스키마)
     */
    @Override
    public void sendMemberRecommendationEmail(MemberRecommendation memberRecommendation) {
        String memberEmail = memberRecommendation.getMember().getEmail();
        if (memberEmail == null || memberEmail.isBlank()) {
            throw new IllegalArgumentException("회원 이메일이 없습니다");
        }

        String subject = buildMemberRecommendationSubject(memberRecommendation);
        String htmlContent = buildMemberRecommendationHtml(memberRecommendation, subject);
        String plainText = buildMemberRecommendationContent(memberRecommendation);

        try {
            MimeMessageHelper helper = createMimeMessageHelper();
            helper.setFrom(getFromAddress());
            helper.setTo(memberEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlContent);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            throw new RuntimeException("개인 추천 HTML 메일 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개인 추천 이메일 HTML 내용 생성 (신규 스키마)
     */
    private String buildMemberRecommendationHtml(MemberRecommendation memberRecommendation, String subject) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("recommendationDate",
                memberRecommendation.getRecommendation().getCreatedAt().format(DATE_FORMATTER));

        // 팀 페이지 URL 생성 (코드메이트 웹 유도)
        String teamPageUrl = buildTeamPageUrl(memberRecommendation.getTeamId());
        context.setVariable("teamPageUrl", teamPageUrl);

        // Prepare problem view models for template
        List<ProblemView> problems = memberRecommendation.getRecommendation().getProblems().stream()
                .map(rp -> {
                    Problem p = rp.getProblem();
                    return new ProblemView(
                            memberRecommendation.getRecommendation().getProblems().indexOf(rp) + 1,
                            p.getTitleKo(),
                            p.getLevel(),
                            p.getUrl(),
                            p.getId(),
                            p.getAcceptedUserCount(),
                            p.getAverageTries(),
                            false  // TODO: 완료 여부 확인 로직 필요
                    );
                })
                .toList();
        context.setVariable("problems", problems);

        // 로고 이미지 추가
        try {
            String base64Image = getBase64EncodedImage("static/images/logo.png");
            context.setVariable("logoImage", base64Image);
        } catch (IOException e) {
            context.setVariable("logoImage", null);
        }

        String htmlContent = templateEngine.process("recommendation-email-v3", context);
        return cssInlinerService.inlineCss(htmlContent, "static/css/email-recommendation-v2.css");
    }

    /**
     * 팀 페이지 URL 생성
     */
    private String buildTeamPageUrl(Long teamId) {
        if (teamId == null) {
            throw new IllegalArgumentException("팀 ID는 null일 수 없습니다");
        }
        return frontendUrl + "/teams/" + teamId;
    }

    /**
     * 개인 추천 이메일 제목 생성 (신규 스키마)
     */
    private String buildMemberRecommendationSubject(MemberRecommendation memberRecommendation) {
        return String.format("[CodeMate] 오늘의 미션 문제 (%s)",
                memberRecommendation.getRecommendation().getCreatedAt().format(DATE_FORMATTER)
        );
    }

    /**
     * 개인 추천 이메일 내용 생성 (신규 스키마)
     */
    private String buildMemberRecommendationContent(MemberRecommendation memberRecommendation) {
        StringBuilder content = new StringBuilder();

        // MemberRecommendation에서 팀 이름 가져오기
        String teamName = memberRecommendation.getTeamName() != null
                ? memberRecommendation.getTeamName()
                : "팀";

        content.append(String.format("안녕하세요! %s팀의 오늘 추천 문제입니다.\n\n", teamName));

        List<RecommendationProblem> problems = memberRecommendation.getRecommendation().getProblems();
        for (int i = 0; i < problems.size(); i++) {
            Problem problem = problems.get(i).getProblem();
            content.append(String.format("%d. %s\n", i + 1, problem.getTitleKo()));
            content.append(String.format("   레벨: %d | URL: %s\n\n",
                    problem.getLevel(), problem.getUrl()));
        }

        content.append("오늘도 화이팅하세요! 💪\n");
        return content.toString();
    }

    /**
     * 이미지를 Base64(data URI)로 인코딩
     */
    private String getBase64EncodedImage(String imagePath) throws IOException {
        Resource resource = new ClassPathResource(imagePath);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return Base64.getEncoder().encodeToString(bytes);
    }
}
