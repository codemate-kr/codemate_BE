package com.ryu.studyhelper.infrastructure.mail;

import com.ryu.studyhelper.infrastructure.mail.support.CssInliner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 이메일 템플릿 미리보기 생성 테스트
 *
 * 실행 방법:
 * 1. 이 테스트를 실행하면 build/email-preview.html 파일이 생성됩니다
 * 2. 생성된 파일을 브라우저로 열면 실제 이메일 모습을 볼 수 있습니다
 */
@SpringBootTest
@Disabled
public class EmailTemplatePreviewTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private CssInliner cssInliner;

    @Test
    public void generateEmailPreview() throws IOException {
        // 테스트 데이터 준비
        Context context = new Context();
        context.setVariable("subject", "[CodeMate] 테스트팀 오늘의 추천 문제 (10/26)");
        context.setVariable("teamName", "테스트팀");
        context.setVariable("recommendationDate", LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MM/dd")));

        // 샘플 문제 데이터
        List<ProblemView> problems = List.of(
                new ProblemView(1, "두 수의 합", 13, "https://www.acmicpc.net/problem/1000",
                        1000L, 50000, 2.3),
                new ProblemView(2, "A+B", 1, "https://www.acmicpc.net/problem/1001",
                        1001L, 100000, 1.5),
                new ProblemView(3, "다이나믹 프로그래밍", 20, "https://www.acmicpc.net/problem/2000",
                        2000L, 5000, 5.8)
        );
        context.setVariable("problems", problems);

        // 팀 페이지 URL (v3 템플릿용)
        context.setVariable("teamPageUrl", "https://codemate.kr/teams/1");

        // Thymeleaf 템플릿 렌더링
        String htmlContent = templateEngine.process("recommendation-email-v3", context);

        // CSS 인라인 변환
        String inlinedHtml = cssInliner.inline(htmlContent, "static/css/email-recommendation-v2.css");

        // 파일로 저장
        String outputPath = "build/email-preview.html";
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(inlinedHtml);
        }

        System.out.println("====================================");
        System.out.println("이메일 미리보기 생성 완료!");
        System.out.println("파일 위치: " + outputPath);
        System.out.println("브라우저로 열어서 확인하세요.");
        System.out.println("====================================");
    }

    @Test
    public void generateTeamInvitationEmailPreview() throws IOException {
        Context context = new Context();
        context.setVariable("teamName", "알고리즘 스터디");
        context.setVariable("inviterHandle", "leader123");
        context.setVariable("expiresAt", "2026-01-27 12:00");
        context.setVariable("dashboardUrl", "https://codemate.kr/dashboard");

        String htmlContent = templateEngine.process("team-invitation-email", context);

        String outputPath = "build/team-invitation-preview.html";
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(htmlContent);
        }

        System.out.println("====================================");
        System.out.println("팀 초대 이메일 미리보기 생성 완료!");
        System.out.println("파일 위치: " + outputPath);
        System.out.println("====================================");
    }

    /**
     * 테스트용 ProblemView
     */
    record ProblemView(
            int order,
            String title,
            int level,
            String url,
            Long problemId,
            Integer acceptedUserCount,
            Double averageTries
    ) {
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