package com.ryu.studyhelper.recommendation.mail;

import com.ryu.studyhelper.infrastructure.mail.sender.MailMessage;
import com.ryu.studyhelper.infrastructure.mail.support.CssInliner;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * 추천 메일 빌더
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationMailBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");

    private final TemplateEngine templateEngine;
    private final CssInliner cssInliner;

    @Value("${FRONTEND_URL:https://codemate.kr}")
    private String frontendUrl;

    /**
     * MemberRecommendation으로부터 메일 메시지 생성
     */
    public MailMessage build(MemberRecommendation mr) {
        String to = mr.getMember().getEmail();
        String date = mr.getRecommendation().getCreatedAt().format(DATE_FORMATTER);
        Long teamId = mr.getTeamId();

        return new MailMessage(
                to,
                buildSubject(date),
                buildHtml(mr, date, teamId)
        );
    }

    // ===== private =====

    private String buildSubject(String date) {
        return String.format("[CodeMate] 오늘의 미션 문제 (%s)", date);
    }

    private String buildHtml(MemberRecommendation mr, String date, Long teamId) {
        Context context = new Context();
        context.setVariable("subject", buildSubject(date));
        context.setVariable("recommendationDate", date);
        context.setVariable("teamPageUrl", frontendUrl + "/teams/" + teamId);

        List<RecommendationProblem> rps = mr.getRecommendation().getProblems();
        List<ProblemView> problems = new ArrayList<>();
        for (int i = 0; i < rps.size(); i++) {
            problems.add(ProblemView.from(i + 1, rps.get(i).getProblem()));
        }
        context.setVariable("problems", problems);

        try {
            context.setVariable("logoImage", getBase64EncodedImage("static/images/logo.png"));
        } catch (IOException e) {
            log.warn("로고 이미지 로드 실패", e);
            context.setVariable("logoImage", null);
        }

        String html = templateEngine.process("recommendation-email-v3", context);
        return cssInliner.inline(html, "static/css/email-recommendation-v2.css");
    }

    private String getBase64EncodedImage(String imagePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(imagePath);
        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 템플릿용 ProblemView
     */
    private record ProblemView(
            int order,
            String title,
            int level,
            String url,
            Long problemId,
            Integer acceptedUserCount,
            Double averageTries
    ) {
        static ProblemView from(int order, Problem p) {
            return new ProblemView(
                    order,
                    p.getTitleKo(),
                    p.getLevel(),
                    p.getUrl(),
                    p.getId(),
                    p.getAcceptedUserCount(),
                    p.getAverageTries()
            );
        }

        private static final String[] TIER_NAMES = {
            "Bronze V", "Bronze IV", "Bronze III", "Bronze II", "Bronze I",
            "Silver V", "Silver IV", "Silver III", "Silver II", "Silver I",
            "Gold V", "Gold IV", "Gold III", "Gold II", "Gold I",
            "Platinum V", "Platinum IV", "Platinum III", "Platinum II", "Platinum I",
            "Diamond V", "Diamond IV", "Diamond III", "Diamond II", "Diamond I",
            "Ruby V", "Ruby IV", "Ruby III", "Ruby II", "Ruby I"
        };

        private static final String[] TIER_COLORS = {
            "#ea580c", "#ea580c", "#ea580c", "#ea580c", "#ea580c",  // Bronze
            "#6b7280", "#6b7280", "#6b7280", "#6b7280", "#6b7280",  // Silver
            "#ca8a04", "#ca8a04", "#ca8a04", "#ca8a04", "#ca8a04",  // Gold
            "#0891b2", "#0891b2", "#0891b2", "#0891b2", "#0891b2",  // Platinum
            "#2563eb", "#2563eb", "#2563eb", "#2563eb", "#2563eb",  // Diamond
            "#dc2626", "#dc2626", "#dc2626", "#dc2626", "#dc2626"   // Ruby
        };

        private static final String[] TIER_BG_COLORS = {
            "#fff7ed", "#fff7ed", "#fff7ed", "#fff7ed", "#fff7ed",  // Bronze
            "#f3f4f6", "#f3f4f6", "#f3f4f6", "#f3f4f6", "#f3f4f6",  // Silver
            "#fefce8", "#fefce8", "#fefce8", "#fefce8", "#fefce8",  // Gold
            "#ecfeff", "#ecfeff", "#ecfeff", "#ecfeff", "#ecfeff",  // Platinum
            "#eff6ff", "#eff6ff", "#eff6ff", "#eff6ff", "#eff6ff",  // Diamond
            "#fef2f2", "#fef2f2", "#fef2f2", "#fef2f2", "#fef2f2"   // Ruby
        };

        public String getTierName() {
            if (level <= 0 || level > 30) return "Unrated";
            return TIER_NAMES[level - 1];
        }

        public String getTierColor() {
            if (level <= 0 || level > 30) return "#6b7280";
            return TIER_COLORS[level - 1];
        }

        public String getTierBgColor() {
            if (level <= 0 || level > 30) return "#f3f4f6";
            return TIER_BG_COLORS[level - 1];
        }

        public String getFormattedAverageTries() {
            return String.format("%.1f", averageTries != null ? averageTries : 0.0);
        }
    }
}