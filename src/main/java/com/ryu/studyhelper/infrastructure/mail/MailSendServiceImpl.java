package com.ryu.studyhelper.infrastructure.mail;

import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.MailTxtSendDto;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.TeamRecommendationProblem;
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
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public MailSendServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Value("${spring.mail.username}")    // 애플리케이션 설정에 꼭 있어야 함
    private String EMAIL_SENDER;

    /**
     * 텍스트 기반 메일 전송
     */
    @Override
    public void sendTxtEmail(MailTxtSendDto mailTxtSendDto) {
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setTo(mailTxtSendDto.getEmailAddr());      // 받는 사람
//        smm.setFrom(EMAIL_SENDER);
        smm.setFrom("StudyHalp <" + EMAIL_SENDER + ">");// 보내는 사람
        smm.setSubject(mailTxtSendDto.getSubject());   // 제목
        smm.setText(mailTxtSendDto.getContent());      // 내용(plain text)
        try {
            mailSender.send(smm);
        } catch (MailException e) {
            // 로깅으로 교체 권장
            throw e;
        }
    }

    /**
     * HTML 템플릿 기반 메일 전송
     */
    @Override
    public void sendHtmlEmail(MailHtmlSendDto mailHtmlSendDto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // 두 번째 파라미터 true = multipart(이미지 첨부/인라인 등 가능)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            // Thymeleaf 템플릿 컨텍스트 구성
            Context context = new Context();
            context.setVariable("subject", mailHtmlSendDto.getSubject());
            context.setVariable("message", mailHtmlSendDto.getContent());
            if ("user".equalsIgnoreCase(mailHtmlSendDto.getTarget())) {
                context.setVariable("userType", "일반 사용자");
            } else if ("admin".equalsIgnoreCase(mailHtmlSendDto.getTarget())) {
                context.setVariable("userType", "관리자");
            } else {
                context.setVariable("userType", "사용자");
            }

            // 로고를 Base64로 인라인 주입 (템플릿에서 data URI로 사용)
            try {
                String base64Image = getBase64EncodedImage("static/images/logo.png");
                context.setVariable("logoImage", base64Image);
            } catch (IOException e) {
                // 로고 이미지가 없어도 이메일 전송은 계속 진행
                context.setVariable("logoImage", null);
            }

            // templates/email-template.html 이어야 함 (확장자 없이 논리명)
            String htmlContent = templateEngine.process("email-template", context);

            helper.setFrom(EMAIL_SENDER);
            helper.setTo(mailHtmlSendDto.getEmailAddr());
            helper.setSubject(mailHtmlSendDto.getSubject());
            helper.setText(htmlContent, true); // HTML로 전송

            mailSender.send(message);
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

        try {
            // Build HTML content from Thymeleaf template
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("teamName", recommendation.getTeam().getName());
            context.setVariable("recommendationDate", recommendation.getRecommendationDate()
                    .format(DateTimeFormatter.ofPattern("MM/dd")));

            // Prepare problem view models for template
            List<ProblemView> problems = recommendation.getProblems().stream()
                    .map(trp -> new ProblemView(
                            trp.getRecommendationOrder(),
                            trp.getProblem().getTitleKo(),
                            trp.getProblem().getLevel(),
                            trp.getProblem().getUrl()
                    ))
                    .toList();
            context.setVariable("problems", problems);

            // 로고 이미지 추가
            try {
                String base64Image = getBase64EncodedImage("static/images/logo.png");
                context.setVariable("logoImage", base64Image);
            } catch (IOException e) {
                context.setVariable("logoImage", null);
            }

            String htmlContent = templateEngine.process("recommendation-email-template", context);
            String plainText = buildRecommendationContent(recommendation);

            // Send email to each recipient as multipart/alternative
            for (String email : memberEmails) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom("StudyHalp <" + EMAIL_SENDER + ">");
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(plainText, htmlContent);
                mailSender.send(message);
            }
        } catch (MessagingException e) {
            throw new RuntimeException("추천 HTML 메일 전송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 추천 이메일 제목 생성
     */
    private String buildRecommendationSubject(TeamRecommendation recommendation) {
        return String.format("[StudyHalp] %s팀 오늘의 추천 문제 (%s)",
                recommendation.getTeam().getName(),
                recommendation.getRecommendationDate().format(DateTimeFormatter.ofPattern("MM/dd"))
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

    // Template view for problems
    private static class ProblemView {
        private final Integer order;
        private final String title;
        private final Integer level;
        private final String url;

        public ProblemView(Integer order, String title, Integer level, String url) {
            this.order = order;
            this.title = title;
            this.level = level;
            this.url = url;
        }

        public Integer getOrder() { return order; }
        public String getTitle() { return title; }
        public Integer getLevel() { return level; }
        public String getUrl() { return url; }
    }

    // 이미지를 Base64(data URI)로 인코딩
    private String getBase64EncodedImage(String imagePath) throws IOException {
        Resource resource = new ClassPathResource(imagePath);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return Base64.getEncoder().encodeToString(bytes);
    }
}
